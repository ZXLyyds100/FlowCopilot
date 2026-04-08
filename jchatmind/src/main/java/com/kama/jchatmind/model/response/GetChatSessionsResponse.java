package com.kama.jchatmind.model.response;

import com.kama.jchatmind.model.vo.ChatSessionVO;
import lombok.Builder;
import lombok.Data;

/**
 * 聊天会话列表响应对象。
 */
@Data
@Builder
public class GetChatSessionsResponse {
    private ChatSessionVO[] chatSessions;
}
