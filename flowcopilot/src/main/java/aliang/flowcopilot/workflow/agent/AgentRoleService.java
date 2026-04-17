package aliang.flowcopilot.workflow.agent;

import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

/**
 * Provides role boundaries for workflow nodes.
 */
@Service
public class AgentRoleService {

    private final Map<WorkflowAgentRole, WorkflowAgentProfile> profiles = new EnumMap<>(WorkflowAgentRole.class);

    public AgentRoleService() {
        register(WorkflowAgentRole.PLANNER, "Planner Agent", "理解任务、拆解步骤、确定任务类型。", """
                你是 FlowCopilot 的 Planner Agent。
                你只负责理解用户任务、识别任务类型，并输出清晰、可执行的分阶段计划。
                输出必须结构化，避免直接完成最终内容。
                """);
        register(WorkflowAgentRole.RETRIEVER, "Retriever Agent", "围绕计划检索知识库与上下文，给出可引用资料。", """
                你是 FlowCopilot 的 Retriever Agent。
                你只负责判断需要哪些知识、整理检索结果和引用依据。
                不要直接写最终报告。
                """);
        register(WorkflowAgentRole.EXECUTOR, "Executor Agent", "基于计划和资料生成可交付初稿。", """
                你是 FlowCopilot 的 Executor Agent。
                你负责根据任务目标、执行计划和引用资料生成一版结构完整的初稿。
                输出应可直接交给 Reviewer Agent 复核。
                """);
        register(WorkflowAgentRole.REVIEWER, "Reviewer Agent", "复核初稿质量、指出风险并给出修改建议。", """
                你是 FlowCopilot 的 Reviewer Agent。
                你负责检查初稿是否覆盖用户目标、是否引用了资料、结构是否清晰。
                输出必须包含质量判断、问题和修改建议。
                """);
        register(WorkflowAgentRole.REPORTER, "Reporter Agent", "整理最终可交付产物。", """
                你是 FlowCopilot 的 Reporter Agent。
                你负责把初稿、复核意见和引用资料整理成最终 Markdown 交付物。
                """);
    }

    public WorkflowAgentProfile getProfile(WorkflowAgentRole role) {
        return profiles.get(role);
    }

    private void register(WorkflowAgentRole role, String displayName, String responsibility, String systemPrompt) {
        profiles.put(role, WorkflowAgentProfile.builder()
                .role(role)
                .displayName(displayName)
                .responsibility(responsibility)
                .systemPrompt(systemPrompt.strip())
                .build());
    }
}
