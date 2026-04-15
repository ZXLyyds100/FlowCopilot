package com.kama.jchatmind.config;

import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 澶氭ā鍨嬮厤缃被銆?
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(JChatMindLlmProperties.class)
public class MultiChatModelConfig {

    @Bean
    public ChatModelRegistry chatModelRegistry(JChatMindLlmProperties properties) {
        Map<String, ChatModel> chatModels = new LinkedHashMap<>();
        registerDeepSeek(properties.getDeepseek(), chatModels);
        registerZhipu(properties.getZhipu(), chatModels);
        return new ChatModelRegistry(chatModels);
    }

    private void registerDeepSeek(JChatMindLlmProperties.ProviderProperties config, Map<String, ChatModel> chatModels) {
        if (!config.isUsable()) {
            logProviderSkip("deepseek-chat", config);
            return;
        }
        ChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModel())
                .timeout(normalizeTimeout(config.getTimeout()))
                .logRequests(config.isLogRequests())
                .logResponses(config.isLogResponses())
                .build();
        chatModels.put("deepseek-chat", chatModel);
    }

    private void registerZhipu(JChatMindLlmProperties.ProviderProperties config, Map<String, ChatModel> chatModels) {
        if (!config.isUsable()) {
            logProviderSkip("glm-4.6", config);
            return;
        }
        ChatModel chatModel = ZhipuAiChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .model(config.getModel())
                .callTimeout(normalizeTimeout(config.getTimeout()))
                .readTimeout(normalizeTimeout(config.getTimeout()))
                .connectTimeout(normalizeTimeout(config.getTimeout()))
                .writeTimeout(normalizeTimeout(config.getTimeout()))
                .logRequests(config.isLogRequests())
                .logResponses(config.isLogResponses())
                .build();
        chatModels.put("glm-4.6", chatModel);
    }

    private void logProviderSkip(String modelName, JChatMindLlmProperties.ProviderProperties config) {
        if (config.isEnabled()) {
            log.warn("Model {} is enabled but api-key is blank. Skipping registration.", modelName);
        } else {
            log.info("Model {} is disabled. Skipping registration.", modelName);
        }
    }

    private Duration normalizeTimeout(Duration timeout) {
        return timeout == null ? Duration.ofSeconds(60) : timeout;
    }
}
