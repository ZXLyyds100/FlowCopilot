package aliang.flowcopilot.event;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 聊天事件对象。
 * <p>
 * 当用户发送消息后，业务层会发布该事件，
 * 监听器收到后再异步启动对应 Agent 进行处理。
 */
@Data
@AllArgsConstructor
public class ChatEvent {
    private String agentId;
    private String sessionId;
    private String userInput;
}
