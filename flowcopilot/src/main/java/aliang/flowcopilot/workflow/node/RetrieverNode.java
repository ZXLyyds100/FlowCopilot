package aliang.flowcopilot.workflow.node;

import aliang.flowcopilot.workflow.agent.AgentRoleService;
import aliang.flowcopilot.workflow.agent.WorkflowAgentProfile;
import aliang.flowcopilot.workflow.agent.WorkflowAgentRole;
import aliang.flowcopilot.workflow.ai.StructuredOutputService;
import aliang.flowcopilot.workflow.rag.RetrieverService;
import aliang.flowcopilot.workflow.state.WorkflowSource;
import aliang.flowcopilot.workflow.state.WorkflowState;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Retrieves knowledge context for the workflow.
 */
@Component
@AllArgsConstructor
public class RetrieverNode implements WorkflowNode {

    private final RetrieverService retrieverService;
    private final AgentRoleService agentRoleService;
    private final StructuredOutputService structuredOutputService;

    @Override
    public String key() {
        return "retriever";
    }

    @Override
    public String name() {
        return "Retriever Agent";
    }

    @Override
    public WorkflowState execute(WorkflowState state) {
        List<WorkflowSource> sources;
        try {
            sources = retrieverService.retrieve(state.getKnowledgeBaseId(), state.getUserInput());
        } catch (Exception e) {
            sources = retrieverService.retrievalFailedSource(e);
        }
        state.setSources(sources);
        List<String> retrievedContents = new ArrayList<>(sources.stream()
                .map(WorkflowSource::getContent)
                .toList());
        state.setRetrievedContents(retrievedContents);

        WorkflowAgentProfile profile = agentRoleService.getProfile(WorkflowAgentRole.RETRIEVER);
        String retrievalSummary = structuredOutputService.generateOrFallback(profile, """
                用户任务：%s
                任务类型：%s
                执行计划：%s
                召回资料：%s
                请总结这些资料如何支撑后续生成。
                """.formatted(state.getUserInput(), state.getTaskType(), state.getPlan(), state.getRetrievedContents()),
                """
                ## 目标理解
                需要为后续生成提供可引用上下文。

                ## 关键依据
                %s

                ## 结构化结果
                已整理 %d 条可用资料，后续 Executor Agent 应优先基于这些资料生成初稿。
                """.formatted(String.join("\n", state.safeRetrievedContents()), state.safeRetrievedContents().size()).strip());
        retrievedContents.add(0, retrievalSummary);
        state.setRetrievedContents(retrievedContents);
        return state;
    }
}
