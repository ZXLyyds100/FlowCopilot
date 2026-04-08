package com.kama.jchatmind.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 聊天会话前端展示对象。
 */
@Data
@Builder
public class ChatSessionVO {
    private String id;
    private String agentId;
    private String title;
}
