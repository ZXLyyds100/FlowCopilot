package aliang.flowcopilot.workflow.rag;

import aliang.flowcopilot.service.RagService;
import aliang.flowcopilot.workflow.observability.ObservationRecorder;
import aliang.flowcopilot.workflow.observability.ObservationScope;
import aliang.flowcopilot.workflow.observability.ObservationSpanType;
import aliang.flowcopilot.workflow.observability.ObservationStatus;
import aliang.flowcopilot.workflow.state.WorkflowSource;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Workflow-level retrieval service that turns RAG into a standard node capability.
 */
@Service
@AllArgsConstructor
public class RetrieverService {

    private final RagService ragService;
    private final KnowledgeIndexService knowledgeIndexService;
    private final RetrievalRecordService retrievalRecordService;
    private final ObservationRecorder observationRecorder;

    public List<WorkflowSource> retrieve(String knowledgeBaseId, String query) {
        ObservationScope retrievalScope = observationRecorder.startChildSpan(
                ObservationSpanType.RETRIEVAL_CALL,
                "Knowledge retrieval",
                "retriever",
                query,
                retrievalAttributes(knowledgeBaseId)
        );
        try {
            List<WorkflowSource> sources;
            if (!knowledgeIndexService.hasKnowledgeScope(knowledgeBaseId)) {
                sources = List.of(retrievalRecordService.fallbackSource("No knowledge base selected; fallback to user input and graph template."));
            } else {
                List<String> hits = ragService.similaritySearch(knowledgeBaseId, query);
                if (hits.isEmpty()) {
                    sources = List.of(retrievalRecordService.fallbackSource("No related knowledge hits were found."));
                } else {
                    sources = retrievalRecordService.fromKnowledgeHits(hits);
                }
            }
            observationRecorder.complete(
                    retrievalScope,
                    ObservationStatus.COMPLETED,
                    "Retrieved sources: " + sources.size(),
                    Map.of("sourceCount", sources.size())
            );
            return sources;
        } catch (Exception e) {
            observationRecorder.fail(retrievalScope, e, Map.of("knowledgeBaseId", knowledgeBaseId == null ? "" : knowledgeBaseId));
            throw e;
        }
    }

    public List<WorkflowSource> retrievalFailedSource(Exception e) {
        return List.of(retrievalRecordService.fallbackSource("Retrieval temporarily unavailable: " + e.getMessage()));
    }

    private Map<String, Object> retrievalAttributes(String knowledgeBaseId) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("knowledgeBaseId", knowledgeBaseId == null ? "" : knowledgeBaseId);
        attributes.put("stageKind", "knowledge_retrieval");
        return attributes;
    }
}
