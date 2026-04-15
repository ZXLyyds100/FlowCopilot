package com.kama.jchatmind.agent.examples;

import com.kama.jchatmind.agent.AgentState;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * V1 鐗堟湰锛氬熀纭€鑱婂ぉ鍔熻兘绀轰緥銆?
 */
@Slf4j
public class JChatMindV1 {

    protected String name;
    protected String description;
    protected String systemPrompt;
    protected ChatModel chatModel;
    protected ChatMemory chatMemory;
    protected AgentState agentState;
    protected String sessionId;

    private static final Integer DEFAULT_MAX_MESSAGES = 20;

    public JChatMindV1() {
    }

    public JChatMindV1(String name,
                       String description,
                       String systemPrompt,
                       ChatModel chatModel,
                       Integer maxMessages,
                       String sessionId) {
        this.name = name;
        this.description = description;
        this.systemPrompt = systemPrompt;
        this.chatModel = chatModel;
        this.sessionId = sessionId != null ? sessionId : "default-session";
        this.agentState = AgentState.IDLE;
        this.chatMemory = MessageWindowChatMemory.builder()
                .id(this.sessionId)
                .maxMessages(maxMessages != null ? maxMessages : DEFAULT_MAX_MESSAGES)
                .build();

        if (StringUtils.hasLength(systemPrompt)) {
            this.chatMemory.add(SystemMessage.from(systemPrompt));
        }
    }

    public String chat(String userInput) {
        Assert.notNull(userInput, "鐢ㄦ埛杈撳叆涓嶈兘涓虹┖");
        if (agentState != AgentState.IDLE) {
            throw new IllegalStateException("Agent 鐘舵€佷笉鏄?IDLE锛屽綋鍓嶇姸鎬侊細" + agentState);
        }

        try {
            agentState = AgentState.THINKING;
            chatMemory.add(UserMessage.from(userInput));

            ChatResponse response = chatModel.chat(ChatRequest.builder()
                    .messages(chatMemory.messages())
                    .build());

            Assert.notNull(response, "ChatResponse 涓嶈兘涓虹┖");
            AiMessage aiMessage = response.aiMessage();
            Assert.notNull(aiMessage, "AiMessage 涓嶈兘涓虹┖");

            chatMemory.add(aiMessage);
            agentState = AgentState.FINISHED;
            return aiMessage.text();
        } catch (Exception e) {
            agentState = AgentState.ERROR;
            log.error("鑱婂ぉ杩囩▼涓彂鐢熼敊璇?", e);
            throw new RuntimeException("鑱婂ぉ杩囩▼涓彂鐢熼敊璇?", e);
        } finally {
            agentState = AgentState.IDLE;
        }
    }

    public List<ChatMessage> getConversationHistory() {
        return chatMemory.messages();
    }

    public void reset() {
        chatMemory.clear();
        if (StringUtils.hasLength(systemPrompt)) {
            chatMemory.add(SystemMessage.from(systemPrompt));
        }
        agentState = AgentState.IDLE;
    }

    @Override
    public String toString() {
        return "JChatMindV1 {" +
                "name = " + name + ",\n" +
                "description = " + description + ",\n" +
                "systemPrompt = " + systemPrompt + "}";
    }
}
