package com.kama.jchatmind.controller;

import com.kama.jchatmind.service.SseService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试控制器。
 * <p>
 * 提供健康检查和开发期联调用的简单接口。
 */
@RestController
@AllArgsConstructor
public class TestController {

    private final SseService sseService;

    /**
     * 健康检查接口。
     *
     * @return 固定字符串 {@code ok}
     */
    @RequestMapping("/health")
    public String health() {
        return "ok";
    }

    /**
     * SSE 测试接口。
     *
     * @return 固定字符串 {@code ok}
     */
    @GetMapping("/sse-test")
    public String sseTest() {
        return "ok";
    }
}
