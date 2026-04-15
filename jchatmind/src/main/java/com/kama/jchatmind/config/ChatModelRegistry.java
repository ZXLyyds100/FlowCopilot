package com.kama.jchatmind.config;

import dev.langchain4j.model.chat.ChatModel;

import java.util.Collections;
import java.util.Map;

/**
 * ChatModel 娉ㄥ唽琛ㄣ€?
 */
public class ChatModelRegistry {

    private final Map<String, ChatModel> chatModels;

    public ChatModelRegistry(Map<String, ChatModel> chatModels) {
        this.chatModels = Collections.unmodifiableMap(chatModels);
    }

    public ChatModel get(String key) {
        return chatModels.get(key);
    }

    public Map<String, ChatModel> asMap() {
        return chatModels;
    }
}
