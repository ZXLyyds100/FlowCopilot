package com.kama.jchatmind.model.response;

import lombok.Builder;
import lombok.Data;

/**
 * 创建知识库响应对象。
 */
@Data
@Builder
public class CreateKnowledgeBaseResponse {
    private String knowledgeBaseId;
}
