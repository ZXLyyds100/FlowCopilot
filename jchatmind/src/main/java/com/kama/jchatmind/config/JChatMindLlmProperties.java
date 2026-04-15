package com.kama.jchatmind.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * LangChain4j 妯″瀷閰嶇疆銆?
 */
@Data
@ConfigurationProperties(prefix = "jchatmind.llm.providers")
public class JChatMindLlmProperties {

    private ProviderProperties deepseek = new ProviderProperties();
    private ProviderProperties zhipu = new ProviderProperties();

    @Data
    public static class ProviderProperties {
        private boolean enabled;
        private String apiKey;
        private String baseUrl;
        private String model;
        private Duration timeout = Duration.ofSeconds(60);
        private boolean logRequests;
        private boolean logResponses;

        public boolean isUsable() {
            return enabled && StringUtils.hasText(apiKey);
        }
    }
}
