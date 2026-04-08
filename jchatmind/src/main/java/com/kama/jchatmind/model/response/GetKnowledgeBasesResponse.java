package com.kama.jchatmind.model.response;

import com.kama.jchatmind.model.vo.KnowledgeBaseVO;
import lombok.Builder;
import lombok.Data;

/**
 * 知识库列表响应对象。
 */
@Data
@Builder
public class GetKnowledgeBasesResponse {
    private KnowledgeBaseVO[] knowledgeBases;
}
