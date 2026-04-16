package aliang.flowcopilot.model.response;

import aliang.flowcopilot.model.vo.ChatSessionVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 单个聊天会话响应对象。
 */
@Data
@AllArgsConstructor
@Builder
public class GetChatSessionResponse {
    private ChatSessionVO chatSession;
}
