package com.kama.jchatmind.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * ChatClient 注册表。
 * <p>
 * 用于按名称查找已经注册到 Spring 容器中的大模型客户端，
 * 使运行时能够根据 Agent 配置动态切换模型实现。
 */
@Component
public class ChatClientRegistry {

    private final Map<String, ChatClient> chatClients;

    /**
     * 构造注册表并接收全部 ChatClient Bean。
     *
     * @param chatClients 以 Bean 名为键、ChatClient 为值的映射
     */
    public ChatClientRegistry(Map<String, ChatClient> chatClients) {
        this.chatClients = chatClients;
    }

    /**
     * 按模型标识获取 ChatClient。
     *
     * @param key 模型标识，通常与 Bean 名一致
     * @return 对应的 ChatClient；不存在时返回 {@code null}
     */
    public ChatClient get(String key) {
        return chatClients.get(key);
    }
}
