package aliang.flowcopilot.model.request;

import lombok.Data;

/**
 * 更新知识库请求对象。
 */
@Data
public class UpdateKnowledgeBaseRequest {
    private String name;
    private String description;
}
