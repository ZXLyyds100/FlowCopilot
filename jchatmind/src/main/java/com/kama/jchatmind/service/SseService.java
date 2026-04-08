package com.kama.jchatmind.service;

import com.kama.jchatmind.message.SseMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Server-Sent Events 服务接口。
 * <p>
 * 负责建立会话级别的实时推送通道，并向前端发送 Agent 执行过程中的状态与消息。
 */
public interface SseService {
    /**
     * 建立会话对应的 SSE 长连接。
     * <p>
     * 当前项目没有独立用户体系，因此直接使用 {@code chatSessionId} 作为连接标识。
     *
     * @param chatSessionId 会话 ID
     * @return 可持续推送消息的 SSE 发射器
     */
    SseEmitter connect(String chatSessionId);

    /**
     * 向指定会话对应的客户端推送消息。
     *
     * @param chatSessionId 会话 ID
     * @param message 待发送的 SSE 业务消息
     */
    void send(String chatSessionId, SseMessage message);
}
