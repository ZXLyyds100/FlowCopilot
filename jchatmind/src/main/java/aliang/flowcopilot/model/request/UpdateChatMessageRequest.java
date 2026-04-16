package aliang.flowcopilot.model.request;

import aliang.flowcopilot.model.dto.ChatMessageDTO;
import lombok.Data;

/**
 * 更新聊天消息请求对象。
 */
@Data
public class UpdateChatMessageRequest {
    private String content;
    private ChatMessageDTO.MetaData metadata;
}
