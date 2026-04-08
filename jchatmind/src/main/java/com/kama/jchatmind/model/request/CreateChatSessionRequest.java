package com.kama.jchatmind.model.request;

import lombok.Data;

/**
 * 创建聊天会话请求对象。
 */
@Data
public class CreateChatSessionRequest {
    private String agentId;
    private String title;
}
