package aliang.flowcopilot.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档 DTO。
 * <p>
 * 在业务层表示知识库中的文档记录，并携带文件路径等附加信息。
 */
@Data
@Builder
public class DocumentDTO {
    private String id;

    private String kbId;

    private String filename;

    private String filetype;

    private Long size;

    private MetaData metadata;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 文档附加元数据。
     */
    @Data
    public static class MetaData {
        private String filePath; // 文件存储路径
    }
}
