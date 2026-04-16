package aliang.flowcopilot.model.response;

import lombok.Builder;
import lombok.Data;

/**
 * 创建聊天会话响应对象。
 */
@Data
@Builder
public class CreateChatSessionResponse {
    private String chatSessionId;
}
