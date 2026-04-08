package com.kama.jchatmind.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 多模型客户端配置类。
 * <p>
 * 将不同供应商的大模型封装成统一的 {@link ChatClient} Bean，
 * 并以模型名称作为 Bean 名，便于运行时按 Agent 配置动态查找。
 */
@Configuration
public class MultiChatClientConfig {
    /**
     * 注册 DeepSeek 对话客户端。
     *
     * @param deepSeekChatModel Spring AI 自动装配的 DeepSeek 模型对象
     * @return 对应的 ChatClient
     */
    @Bean("deepseek-chat")
    public ChatClient deepSeekChatClient(DeepSeekChatModel deepSeekChatModel) {
        return ChatClient.create(deepSeekChatModel);
    }

    /**
     * 注册智谱对话客户端。
     *
     * @param zhiPuAiChatModel Spring AI 自动装配的智谱模型对象
     * @return 对应的 ChatClient
     */
    @Bean("glm-4.6")
    public ChatClient zhiPuAiChatClient(ZhiPuAiChatModel zhiPuAiChatModel) {
        return ChatClient.create(zhiPuAiChatModel);
    }
}
