package aliang.flowcopilot.workflow.node;

import aliang.flowcopilot.workflow.state.WorkflowState;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Produces the main first-stage task draft from plan and retrieved context.
 */
@Component
public class ExecutorNode implements WorkflowNode {
    @Override
    public String key() {
        return "executor";
    }

    @Override
    public String name() {
        return "Executor";
    }

    @Override
    public WorkflowState execute(WorkflowState state) {
        String context = state.safeRetrievedContents()
                .stream()
                .map(item -> "- " + item)
                .collect(Collectors.joining("\n"));
        String draft = """
                ## 任务理解
                %s

                ## 执行计划
                %s

                ## 参考上下文
                %s

                ## 初步结果
                已根据任务目标、计划和可用上下文生成第一阶段可交付结果。后续阶段可接入 Reviewer、Approval 与 LangGraph4j 条件路由增强。
                """.formatted(state.getUserInput(), state.getPlan(), context);
        state.setDraftResult(draft.strip());
        return state;
    }
}
