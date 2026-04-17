package aliang.flowcopilot.event.listener;

import aliang.flowcopilot.agent.JChatMind;
import aliang.flowcopilot.agent.JChatMindFactory;
import aliang.flowcopilot.event.ChatEvent;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 聊天事件监听器。
 * <p>
 * 负责在后台线程中消费聊天事件，并创建运行时 Agent 执行完整的推理流程。
 */
@Component
@AllArgsConstructor
public class ChatEventListener {

    private final JChatMindFactory jChatMindFactory;

    /**
     * 异步处理聊天事件。
     *
     * @param event 聊天事件
     */
    @Async
    @EventListener
    public void handle(ChatEvent event) {
        // 创建一个 Agent 实例处理聊天事件
        JChatMind jChatMind = jChatMindFactory.create(event.getAgentId(), event.getSessionId());
        jChatMind.run();
    }
}
