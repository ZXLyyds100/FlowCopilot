package aliang.flowcopilot.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import aliang.flowcopilot.message.SseMessage;
import aliang.flowcopilot.service.SseService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SSE 服务实现。
 * <p>
 * 负责维护按会话维度组织的长连接，并将 Agent 的状态和结果实时推送给前端。
 */
@Service
@AllArgsConstructor
public class SseServiceImpl implements SseService {

    private final ConcurrentMap<String, SseEmitter> clients = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Override
    /**
     * 建立新的 SSE 长连接并注册到会话映射中。
     *
     * @param chatSessionId 会话 ID
     * @return SSE 发射器
     */
    public SseEmitter connect(String chatSessionId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        clients.put(chatSessionId, emitter);

        try {
            emitter.send(SseEmitter.event()
                    .name("init")
                    .data("connected")
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        emitter.onCompletion(() -> {
            clients.remove(chatSessionId);
        });
        emitter.onTimeout(() -> clients.remove(chatSessionId));
        emitter.onError((error) -> clients.remove(chatSessionId));

        return emitter;
    }

    @Override
    /**
     * 向指定会话的客户端推送一条 SSE 消息。
     *
     * @param chatSessionId 会话 ID
     * @param message SSE 业务消息
     */
    public void send(String chatSessionId, SseMessage message) {
        SseEmitter emitter = clients.get(chatSessionId);

        if (emitter != null) {
            try {
                // 将消息转换为字符串
                String sseMessageStr = objectMapper.writeValueAsString(message);
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(sseMessageStr)
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("No client found for chatSessionId: " + chatSessionId);
        }
    }
}
