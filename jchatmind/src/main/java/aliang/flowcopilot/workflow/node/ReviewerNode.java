package aliang.flowcopilot.workflow.node;

import aliang.flowcopilot.workflow.agent.AgentRoleService;
import aliang.flowcopilot.workflow.agent.WorkflowAgentProfile;
import aliang.flowcopilot.workflow.agent.WorkflowAgentRole;
import aliang.flowcopilot.workflow.ai.StructuredOutputService;
import aliang.flowcopilot.workflow.state.WorkflowReview;
import aliang.flowcopilot.workflow.state.WorkflowState;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Reviews the draft before the final report is published.
 */
@Component
@AllArgsConstructor
public class ReviewerNode implements WorkflowNode {

    private final AgentRoleService agentRoleService;
    private final StructuredOutputService structuredOutputService;

    @Override
    public String key() {
        return "reviewer";
    }

    @Override
    public String name() {
        return "Reviewer Agent";
    }

    @Override
    public WorkflowState execute(WorkflowState state) {
        int score = score(state);
        List<String> suggestions = suggestions(state, score);
        boolean passed = score >= 70;
        String fallback = """
                ## 质量判断
                %s，评分：%d/100。

                ## 主要依据
                - 是否覆盖用户任务：%s
                - 是否包含执行计划：%s
                - 是否使用知识引用：%s

                ## 修改建议
                %s
                """.formatted(
                passed ? "通过" : "需要修改",
                score,
                hasText(state.getDraft()) ? "是" : "否",
                hasText(state.getPlan()) ? "是" : "否",
                state.safeSources().isEmpty() ? "否" : "是",
                String.join("\n", suggestions.stream().map(item -> "- " + item).toList())
        ).strip();

        WorkflowAgentProfile profile = agentRoleService.getProfile(WorkflowAgentRole.REVIEWER);
        String comment = structuredOutputService.generateOrFallback(profile, """
                用户任务：%s
                执行计划：%s
                知识引用：%s
                初稿：%s
                请复核初稿质量，给出评分、风险和修改建议。
                """.formatted(state.getUserInput(), state.getPlan(), state.getRetrievedContents(), state.getDraft()), fallback);

        WorkflowReview review = WorkflowReview.builder()
                .score(score)
                .passed(passed)
                .comment(comment)
                .suggestions(suggestions)
                .build();
        state.setReview(review);
        state.setReviewComment(comment);
        return state;
    }

    private int score(WorkflowState state) {
        int score = 50;
        if (hasText(state.getDraft())) {
            score += 20;
        }
        if (hasText(state.getPlan())) {
            score += 10;
        }
        if (!state.safeSources().isEmpty()) {
            score += 10;
        }
        if (state.getDraft() != null && state.getDraft().length() > 300) {
            score += 10;
        }
        return Math.min(score, 100);
    }

    private List<String> suggestions(WorkflowState state, int score) {
        List<String> suggestions = new ArrayList<>();
        if (!hasText(state.getDraft())) {
            suggestions.add("补充初稿内容，确保 Executor Agent 有明确产出。");
        }
        if (state.safeSources().isEmpty()) {
            suggestions.add("补充知识引用来源，让最终结果可追溯。");
        }
        if (score < 90) {
            suggestions.add("在最终产物中保留 Reviewer Agent 的质量意见，便于后续人工确认。");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("当前初稿结构完整，可进入 Reporter Agent 发布。");
        }
        return suggestions;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
