package aliang.flowcopilot.workflow.node;

import aliang.flowcopilot.workflow.state.WorkflowState;
import org.springframework.stereotype.Component;

/**
 * Creates a deterministic first-stage execution plan from user input.
 */
@Component
public class PlannerNode implements WorkflowNode {
    @Override
    public String key() {
        return "planner";
    }

    @Override
    public String name() {
        return "Planner";
    }

    @Override
    public WorkflowState execute(WorkflowState state) {
        String plan = """
                1. 明确用户输入的任务目标。
                2. 从知识库或上下文中补充必要资料。
                3. 基于资料生成可交付的初稿。
                4. 将初稿整理成最终结果并归档为产物。
                """;
        state.setPlan(plan.strip());
        return state;
    }
}
