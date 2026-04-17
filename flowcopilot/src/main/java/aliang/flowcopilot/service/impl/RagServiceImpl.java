package aliang.flowcopilot.service.impl;

import aliang.flowcopilot.mapper.ChunkBgeM3Mapper;
import aliang.flowcopilot.model.entity.ChunkBgeM3;
import aliang.flowcopilot.service.RagService;
import aliang.flowcopilot.workflow.observability.ObservationRecorder;
import aliang.flowcopilot.workflow.observability.ObservationScope;
import aliang.flowcopilot.workflow.observability.ObservationSpanType;
import aliang.flowcopilot.workflow.observability.ObservationStatus;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG service implementation.
 * <p>
 * It generates embeddings with a local model and queries pgvector for similarity search.
 */
@Service
public class RagServiceImpl implements RagService {

    private static final String EMBEDDING_MODEL = "bge-m3";
    private static final int DEFAULT_TOP_K = 3;

    private final WebClient webClient;
    private final ChunkBgeM3Mapper chunkBgeM3Mapper;
    private final ObservationRecorder observationRecorder;

    public RagServiceImpl(WebClient.Builder builder,
                          ChunkBgeM3Mapper chunkBgeM3Mapper,
                          ObservationRecorder observationRecorder) {
        this.webClient = builder.baseUrl("http://localhost:11434").build();
        this.chunkBgeM3Mapper = chunkBgeM3Mapper;
        this.observationRecorder = observationRecorder;
    }

    @Data
    private static class EmbeddingResponse {
        private float[] embedding;
    }

    private float[] doEmbed(String text) {
        EmbeddingResponse response = webClient.post()
                .uri("/api/embeddings")
                .bodyValue(Map.of(
                        "model", EMBEDDING_MODEL,
                        "prompt", text
                ))
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .block();
        Assert.notNull(response, "Embedding response cannot be null");
        return response.getEmbedding();
    }

    private float[] embedWithObservation(String text, String spanName, Map<String, Object> extraAttributes) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("stageKind", "vectorization");
        attributes.put("embeddingModel", EMBEDDING_MODEL);
        if (extraAttributes != null) {
            attributes.putAll(extraAttributes);
        }

        ObservationScope scope = observationRecorder.startChildSpan(
                ObservationSpanType.LLM_CALL,
                spanName,
                "retriever",
                text,
                attributes
        );

        try {
            float[] embedding = doEmbed(text);
            Map<String, Object> resultAttributes = new LinkedHashMap<>(attributes);
            resultAttributes.put("embeddingDimensions", embedding.length);
            observationRecorder.complete(
                    scope,
                    ObservationStatus.COMPLETED,
                    "Embedding generated",
                    resultAttributes
            );
            return embedding;
        } catch (Exception e) {
            observationRecorder.fail(scope, e, attributes);
            throw e;
        }
    }

    @Override
    public float[] embed(String text) {
        return embedWithObservation(text, "Text vectorization", Map.of(
                "vectorizationScope", "generic"
        ));
    }

    @Override
    public List<String> similaritySearch(String kbId, String title) {
        float[] embedding = embedWithObservation(title, "Query vectorization", Map.of(
                "knowledgeBaseId", kbId == null ? "" : kbId,
                "vectorizationScope", "query"
        ));
        String queryEmbedding = toPgVector(embedding);

        Map<String, Object> searchAttributes = new LinkedHashMap<>();
        searchAttributes.put("stageKind", "vector_search");
        searchAttributes.put("knowledgeBaseId", kbId == null ? "" : kbId);
        searchAttributes.put("topK", DEFAULT_TOP_K);
        searchAttributes.put("searchBackend", "pgvector");

        ObservationScope searchScope = observationRecorder.startChildSpan(
                ObservationSpanType.TOOL_CALL,
                "Vector search",
                "retriever",
                title,
                searchAttributes
        );

        try {
            List<ChunkBgeM3> chunks = chunkBgeM3Mapper.similaritySearch(kbId, queryEmbedding, DEFAULT_TOP_K);
            Map<String, Object> resultAttributes = new LinkedHashMap<>(searchAttributes);
            resultAttributes.put("hitCount", chunks.size());
            observationRecorder.complete(
                    searchScope,
                    ObservationStatus.COMPLETED,
                    "Matched chunks: " + chunks.size(),
                    resultAttributes
            );
            return chunks.stream().map(ChunkBgeM3::getContent).toList();
        } catch (Exception e) {
            observationRecorder.fail(searchScope, e, searchAttributes);
            throw e;
        }
    }

    private String toPgVector(float[] values) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            builder.append(values[i]);
            if (i < values.length - 1) {
                builder.append(",");
            }
        }
        builder.append("]");
        return builder.toString();
    }
}
