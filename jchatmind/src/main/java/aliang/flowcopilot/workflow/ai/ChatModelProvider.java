package aliang.flowcopilot.workflow.ai;

import aliang.flowcopilot.config.ChatModelRegistry;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Selects the default LangChain4j chat model for workflow nodes.
 */
@Service
@AllArgsConstructor
public class ChatModelProvider {

    private final ChatModelRegistry chatModelRegistry;

    public Optional<ChatModel> defaultModel() {
        ChatModel deepseek = chatModelRegistry.get("deepseek-chat");
        if (deepseek != null) {
            return Optional.of(deepseek);
        }
        ChatModel zhipu = chatModelRegistry.get("glm-4.6");
        if (zhipu != null) {
            return Optional.of(zhipu);
        }
        return chatModelRegistry.asMap().values().stream().findFirst();
    }

    public String chat(String systemPrompt, String userPrompt) {
        ChatModel model = defaultModel()
                .orElseThrow(() -> new IllegalStateException("No usable LangChain4j ChatModel configured"));
        ChatResponse response = model.chat(ChatRequest.builder()
                .messages(List.of(
                        SystemMessage.from(systemPrompt),
                        UserMessage.from(userPrompt)
                ))
                .build());
        if (response == null || response.aiMessage() == null) {
            throw new IllegalStateException("LangChain4j returned empty chat response");
        }
        return response.aiMessage().text();
    }
}
