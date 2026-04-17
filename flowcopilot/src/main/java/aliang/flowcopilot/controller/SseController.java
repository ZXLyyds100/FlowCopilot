package aliang.flowcopilot.controller;

import aliang.flowcopilot.service.SseService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 控制器。
 * <p>
 * 为前端建立基于聊天会话的实时推送通道，Agent 执行状态和生成结果都会走这条链路返回。
 */
@RestController
@RequestMapping("/sse")
@AllArgsConstructor
public class SseController {

    private final SseService sseService;

    /**
     * 建立指定会话的 SSE 连接。
     *
     * @param chatSessionId 会话 ID
     * @return SSE 发射器
     */
    @RequestMapping(value = "/connect/{chatSessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@PathVariable String chatSessionId) {
        return sseService.connect(chatSessionId);
    }
}
