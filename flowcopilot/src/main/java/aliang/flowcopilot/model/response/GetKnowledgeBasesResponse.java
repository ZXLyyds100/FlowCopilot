package aliang.flowcopilot.model.response;

import aliang.flowcopilot.model.vo.KnowledgeBaseVO;
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
