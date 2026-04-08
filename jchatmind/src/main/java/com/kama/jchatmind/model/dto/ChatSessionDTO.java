package com.kama.jchatmind.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话 DTO。
 * <p>
 * 表示业务层内部使用的会话对象，包含与 Agent 的关联关系和扩展元数据。
 */
@Data
@Builder
public class ChatSessionDTO {
    private String id;

    private String agentId;

    private String title;

    private MetaData metadata;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 会话扩展元数据。
     */
    @Data
    public static class MetaData {
    }
}
