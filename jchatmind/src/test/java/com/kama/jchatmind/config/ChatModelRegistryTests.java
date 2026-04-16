package com.kama.jchatmind.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChatModelRegistryTests {

    private final MultiChatModelConfig config = new MultiChatModelConfig();

    @Test
    void shouldRegisterConfiguredModels() {
        JChatMindLlmProperties properties = new JChatMindLlmProperties();

        properties.getDeepseek().setEnabled(true);
        properties.getDeepseek().setApiKey("deepseek-test-key");
        properties.getDeepseek().setBaseUrl("https://api.deepseek.com");
        properties.getDeepseek().setModel("deepseek-chat");

        properties.getZhipu().setEnabled(true);
        properties.getZhipu().setApiKey("zhipu-test-key");
        properties.getZhipu().setBaseUrl("https://open.bigmodel.cn/api/paas/v4");
        properties.getZhipu().setModel("glm-4.6");

        ChatModelRegistry registry = config.chatModelRegistry(properties);

        assertNotNull(registry.get("deepseek-chat"));
        assertNotNull(registry.get("glm-4.6"));
    }

    @Test
    void shouldSkipProviderWithoutApiKey() {
        JChatMindLlmProperties properties = new JChatMindLlmProperties();
        properties.getDeepseek().setEnabled(true);
        properties.getDeepseek().setApiKey("");
        properties.getDeepseek().setModel("deepseek-chat");

        ChatModelRegistry registry = config.chatModelRegistry(properties);

        assertNull(registry.get("deepseek-chat"));
    }
}
