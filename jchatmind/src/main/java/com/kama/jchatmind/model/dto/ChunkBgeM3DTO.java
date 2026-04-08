package com.kama.jchatmind.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 向量分块 DTO。
 * <p>
 * 表示知识库文档切块后的运行时对象，包含正文、向量和所属知识库/文档关系。
 */
@Data
@Builder
public class ChunkBgeM3DTO {
    private String id;

    private String kbId;

    private String docId;

    private String content;

    private MetaData metadata;

    private float[] embedding;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 分块扩展元数据。
     */
    @Data
    public static class MetaData {
    }
}
