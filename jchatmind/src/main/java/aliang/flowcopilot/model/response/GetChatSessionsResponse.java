package aliang.flowcopilot.model.response;

import aliang.flowcopilot.model.vo.ChatSessionVO;
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
