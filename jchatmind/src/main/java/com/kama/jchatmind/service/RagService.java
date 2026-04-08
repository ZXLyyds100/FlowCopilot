package com.kama.jchatmind.service;

import java.util.List;

/**
 * RAG 能力抽象接口。
 * <p>
 * 负责文本向量化与向量检索，为知识库工具提供统一访问入口。
 */
public interface RagService {
    /**
     * 将文本编码为向量。
     *
     * @param text 待编码文本
     * @return 浮点向量结果
     */
    float[] embed(String text);

    /**
     * 在指定知识库中执行相似度检索。
     *
     * @param kbId 知识库 ID
     * @param title 查询文本
     * @return 检索命中的内容片段列表
     */
    List<String> similaritySearch(String kbId, String title);
}
