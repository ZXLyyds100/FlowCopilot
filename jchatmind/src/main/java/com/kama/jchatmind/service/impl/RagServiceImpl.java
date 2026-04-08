package com.kama.jchatmind.service.impl;

import com.kama.jchatmind.mapper.ChunkBgeM3Mapper;
import com.kama.jchatmind.model.entity.ChunkBgeM3;
import com.kama.jchatmind.service.RagService;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * RAG 服务实现。
 * <p>
 * 通过本地 embedding 服务生成向量，并基于 PostgreSQL pgvector 执行相似度检索。
 */
@Service
public class RagServiceImpl implements RagService {

    // 封装本地的模型调用
    private final WebClient webClient;
    private final ChunkBgeM3Mapper chunkBgeM3Mapper;

    /**
     * 构造 RAG 服务。
     *
     * @param builder WebClient 构造器
     * @param chunkBgeM3Mapper 向量分块访问接口
     */
    public RagServiceImpl(WebClient.Builder builder, ChunkBgeM3Mapper chunkBgeM3Mapper) {
        this.webClient = builder.baseUrl("http://localhost:11434").build();
        this.chunkBgeM3Mapper = chunkBgeM3Mapper;
    }

    /**
     * Embedding 服务响应结构。
     */
    @Data
    private static class EmbeddingResponse {
        private float[] embedding;
    }

    /**
     * 调用本地 embedding 服务，将文本转换为向量。
     *
     * @param text 待编码文本
     * @return 文本向量
     */
    private float[] doEmbed(String text) {
        EmbeddingResponse resp = webClient.post()
                .uri("/api/embeddings")
                .bodyValue(Map.of(
                        "model", "bge-m3",
                        "prompt", text
                ))
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .block();
        Assert.notNull(resp, "Embedding response cannot be null");
        return resp.getEmbedding();
    }

    @Override
    /**
     * 对外暴露文本向量化能力。
     *
     * @param text 待编码文本
     * @return 向量结果
     */
    public float[] embed(String text) {
        return doEmbed(text);
    }

    @Override
    /**
     * 在指定知识库下执行相似度检索。
     *
     * @param kbId 知识库 ID
     * @param title 查询文本
     * @return 命中的内容片段
     */
    public List<String> similaritySearch(String kbId, String title) {
        String queryEmbedding = toPgVector(doEmbed(title));
        List<ChunkBgeM3> chunks = chunkBgeM3Mapper.similaritySearch(kbId, queryEmbedding, 3);
        return chunks.stream().map(ChunkBgeM3::getContent).toList();
    }

    /**
     * 将向量数组转为 PostgreSQL vector literal 格式。
     *
     * @param v 浮点向量
     * @return PostgreSQL 可识别的向量字符串
     */
    private String toPgVector(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            sb.append(v[i]);
            if (i < v.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
