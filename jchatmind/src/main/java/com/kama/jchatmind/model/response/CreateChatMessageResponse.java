package com.kama.jchatmind.model.response;

import lombok.Builder;
import lombok.Data;

/**
 * 创建聊天消息响应对象。
 */
@Data
@Builder
public class CreateChatMessageResponse {
    private String chatMessageId;
}
