package aliang.flowcopilot.workflow.node;

import aliang.flowcopilot.workflow.agent.AgentRoleService;
import aliang.flowcopilot.workflow.agent.WorkflowAgentProfile;
import aliang.flowcopilot.workflow.agent.WorkflowAgentRole;
import aliang.flowcopilot.workflow.ai.StructuredOutputService;
import aliang.flowcopilot.workflow.state.WorkflowSource;
import aliang.flowcopilot.workflow.state.WorkflowState;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Produces the main second-stage task draft from plan and retrieved context.
 */
@Component
@AllArgsConstructor
public class ExecutorNode implements WorkflowNode {

    private final AgentRoleService agentRoleService;
    private final StructuredOutputService structuredOutputService;

    @Override
    public String key() {
        return "executor";
    }

    @Override
    public String name() {
        return "Executor Agent";
    }

    @Override
    public WorkflowState execute(WorkflowState state) {
        String context = state.safeSources()
                .stream()
                .map(this::formatSource)
                .collect(Collectors.joining("\n"));
        String fallback = """
                ## 任务理解
                %s

                ## 任务类型
                %s

                ## 执行计划
                %s

                ## 知识引用
                %s

                ## 初稿
                已基于 Planner Agent 的计划和 Retriever Agent 的引用资料生成第二阶段协作初稿。
                后续由 Reviewer Agent 复核质量，并由 Reporter Agent 整理最终产物。
                """.formatted(state.getUserInput(), state.getTaskType(), state.getPlan(), context);
        WorkflowAgentProfile profile = agentRoleService.getProfile(WorkflowAgentRole.EXECUTOR);
        String draft = structuredOutputService.generateOrFallback(profile, """
                用户任务：%s
                任务类型：%s
                执行计划：%s
                知识引用：%s
                请生成一份结构完整、可被 Reviewer 复核的初稿。
                """.formatted(state.getUserInput(), state.getTaskType(), state.getPlan(), context), fallback);
        state.setDraft(draft.strip());
        state.setDraftResult(draft.strip());
        return state;
    }

    private String formatSource(WorkflowSource source) {
        return "- [%d] %s：%s".formatted(source.getIndex(), source.getTitle(), source.getContent());
    }
}
