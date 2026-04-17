package aliang.flowcopilot.workflow.service;

import aliang.flowcopilot.workflow.agent.AgentRoleService;
import aliang.flowcopilot.workflow.agent.WorkflowAgentProfile;
import aliang.flowcopilot.workflow.agent.WorkflowAgentRole;
import aliang.flowcopilot.workflow.ai.StructuredOutputService;
import aliang.flowcopilot.workflow.rag.RetrieverService;
import aliang.flowcopilot.workflow.state.WorkflowSource;
import aliang.flowcopilot.workflow.state.WorkflowState;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Business handlers for workflow sub-graph steps.
 */
@Service
@AllArgsConstructor
public class WorkflowSubGraphService {

    private final RetrieverService retrieverService;
    private final AgentRoleService agentRoleService;
    private final StructuredOutputService structuredOutputService;

    public String buildRetrievalBrief(WorkflowState state) {
        WorkflowAgentProfile profile = agentRoleService.getProfile(WorkflowAgentRole.RETRIEVER);
        return structuredOutputService.generateOrFallback(profile, """
                用户任务：%s
                任务类型：%s
                执行计划：%s
                请先输出一段给后续检索与生成共用的检索摘要，明确需要重点补充的事实、证据和引用方向。
                """.formatted(state.getUserInput(), state.getTaskType(), state.getPlan()), """
                ## 检索摘要
                需要围绕用户任务补充关键事实、上下文证据和可引用内容，供后续 Executor 与 Reviewer 使用。
                """.strip());
    }

    public List<WorkflowSource> lookupKnowledge(WorkflowState state) {
        try {
            return retrieverService.retrieve(state.getKnowledgeBaseId(), state.getUserInput());
        } catch (Exception e) {
            return retrieverService.retrievalFailedSource(e);
        }
    }

    public WorkflowState mergeRetrievalResult(WorkflowState state, String retrievalBrief, List<WorkflowSource> sources) {
        state.setSources(sources);
        List<String> retrievedContents = new ArrayList<>();
        if (retrievalBrief != null && !retrievalBrief.isBlank()) {
            retrievedContents.add(retrievalBrief.strip());
        }
        retrievedContents.addAll(sources.stream().map(WorkflowSource::getContent).toList());
        state.setRetrievedContents(retrievedContents);
        return state;
    }
}
