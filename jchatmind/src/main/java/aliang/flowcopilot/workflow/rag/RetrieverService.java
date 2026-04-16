package aliang.flowcopilot.workflow.rag;

import aliang.flowcopilot.service.RagService;
import aliang.flowcopilot.workflow.state.WorkflowSource;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Workflow-level retrieval service that turns RAG into a standard node capability.
 */
@Service
@AllArgsConstructor
public class RetrieverService {

    private final RagService ragService;
    private final KnowledgeIndexService knowledgeIndexService;
    private final RetrievalRecordService retrievalRecordService;

    public List<WorkflowSource> retrieve(String knowledgeBaseId, String query) {
        if (!knowledgeIndexService.hasKnowledgeScope(knowledgeBaseId)) {
            return List.of(retrievalRecordService.fallbackSource("未指定知识库，本轮使用用户输入和通用流程模板执行。"));
        }
        List<String> hits = ragService.similaritySearch(knowledgeBaseId, query);
        if (hits.isEmpty()) {
            return List.of(retrievalRecordService.fallbackSource("知识库未召回相关内容。"));
        }
        return retrievalRecordService.fromKnowledgeHits(hits);
    }

    public List<WorkflowSource> retrievalFailedSource(Exception e) {
        return List.of(retrievalRecordService.fallbackSource("知识检索暂不可用：" + e.getMessage()));
    }
}
