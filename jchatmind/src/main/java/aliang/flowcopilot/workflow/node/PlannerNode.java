package aliang.flowcopilot.workflow.node;

import aliang.flowcopilot.workflow.agent.AgentRoleService;
import aliang.flowcopilot.workflow.agent.WorkflowAgentProfile;
import aliang.flowcopilot.workflow.agent.WorkflowAgentRole;
import aliang.flowcopilot.workflow.ai.StructuredOutputService;
import aliang.flowcopilot.workflow.state.WorkflowState;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Creates a structured second-stage execution plan from user input.
 */
@Component
@AllArgsConstructor
public class PlannerNode implements WorkflowNode {

    private final AgentRoleService agentRoleService;
    private final StructuredOutputService structuredOutputService;

    @Override
    public String key() {
        return "planner";
    }

    @Override
    public String name() {
        return "Planner Agent";
    }

    @Override
    public WorkflowState execute(WorkflowState state) {
        String taskType = inferTaskType(state.getUserInput());
        WorkflowAgentProfile profile = agentRoleService.getProfile(WorkflowAgentRole.PLANNER);
        String fallback = """
                ## 目标理解
                %s

                ## 任务类型
                %s

                ## 执行计划
                1. 明确用户输入的任务目标和交付格式。
                2. 通过 Retriever Agent 检索知识库或补充上下文。
                3. 由 Executor Agent 基于计划和引用资料生成初稿。
                4. 由 Reviewer Agent 复核质量、指出风险和修改建议。
                5. 由 Reporter Agent 整理最终 Markdown 产物。
                """.formatted(state.getUserInput(), taskType).strip();
        String plan = structuredOutputService.generateOrFallback(profile, """
                用户任务：%s
                请判断任务类型，并输出面向后续 Agent 的可执行计划。
                """.formatted(state.getUserInput()), fallback);
        state.setTaskType(taskType);
        state.setPlan(plan.strip());
        return state;
    }

    private String inferTaskType(String userInput) {
        if (userInput == null) {
            return "general_task";
        }
        String normalized = userInput.toLowerCase();
        if (normalized.contains("文档") || normalized.contains("报告") || normalized.contains("介绍") || normalized.contains("答辩")) {
            return "document_generation";
        }
        if (normalized.contains("代码") || normalized.contains("接口") || normalized.contains("实现")) {
            return "engineering_task";
        }
        if (normalized.contains("分析") || normalized.contains("总结")) {
            return "analysis_task";
        }
        return "general_task";
    }
}
