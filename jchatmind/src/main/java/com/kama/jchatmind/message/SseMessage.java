package com.kama.jchatmind.message;

import com.kama.jchatmind.model.vo.ChatMessageVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * SSE 业务消息对象。
 * <p>
 * 用于后端向前端推送 Agent 状态、生成内容和附加元数据。
 */
@Data
@AllArgsConstructor
@Builder
public class SseMessage {

    private Type type;
    private Payload payload;
    private Metadata metadata;

    /**
     * SSE 消息载荷。
     */
    @Data
    @AllArgsConstructor
    @Builder
    public static class Payload {
        private ChatMessageVO message;
        private String statusText;
        private Boolean done;
    }

    /**
     * SSE 消息元数据。
     */
    @Data
    @AllArgsConstructor
    @Builder
    public static class Metadata {
        private String chatMessageId;
    }

    /**
     * SSE 消息类型枚举。
     */
    public enum Type {
        AI_GENERATED_CONTENT,
        AI_PLANNING,
        AI_THINKING,
        AI_EXECUTING,
        AI_DONE,
    }
}
