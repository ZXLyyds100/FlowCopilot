package com.kama.jchatmind.model.response;

import com.kama.jchatmind.model.vo.ChatMessageVO;
import lombok.Builder;
import lombok.Data;

/**
 * 聊天消息列表响应对象。
 */
@Data
@Builder
public class GetChatMessagesResponse {
    private ChatMessageVO[] chatMessages;
}
