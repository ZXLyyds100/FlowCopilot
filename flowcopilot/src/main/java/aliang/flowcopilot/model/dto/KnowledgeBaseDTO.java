package aliang.flowcopilot.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库 DTO。
 * <p>
 * 表示业务层使用的知识库对象，是 Agent 和 RAG 运行时引用知识库的标准形式。
 */
@Data
@Builder
public class KnowledgeBaseDTO {
    private String id;

    private String name;

    private String description;

    private MetaData metadata;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 知识库扩展元数据。
     */
    @Data
    public static class MetaData {
        private String version;
    }

    @Override
    /**
     * 返回便于日志打印的知识库摘要。
     *
     * @return 简要字符串
     */
    public String toString() {
        return "{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
