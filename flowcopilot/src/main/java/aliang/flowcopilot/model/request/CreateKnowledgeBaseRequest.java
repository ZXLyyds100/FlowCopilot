package aliang.flowcopilot.model.request;

import lombok.Data;

/**
 * 创建知识库请求对象。
 */
@Data
public class CreateKnowledgeBaseRequest {
    private String name;
    private String description;
}
