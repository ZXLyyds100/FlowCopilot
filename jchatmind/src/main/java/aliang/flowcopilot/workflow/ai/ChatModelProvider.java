package aliang.flowcopilot.workflow.ai;

import aliang.flowcopilot.config.ChatModelRegistry;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Selects the default LangChain4j chat model for workflow nodes.
 */
@Service
@AllArgsConstructor
public class ChatModelProvider {

    private final ChatModelRegistry chatModelRegistry;

    public ChatCompletionResult chat(String systemPrompt, String userPrompt) {
        SelectedChatModel selectedModel = defaultModel();
        ChatModel model = selectedModel.model();
        ChatResponse response = model.chat(ChatRequest.builder()
                .messages(List.of(
                        SystemMessage.from(systemPrompt),
                        UserMessage.from(userPrompt)
                ))
                .build());
        if (response == null || response.aiMessage() == null) {
            throw new IllegalStateException("LangChain4j returned empty chat response");
        }
        TokenUsage tokenUsage = response.tokenUsage();
        return ChatCompletionResult.builder()
                .text(response.aiMessage().text())
                .modelName(response.modelName() != null && !response.modelName().isBlank()
                        ? response.modelName()
                        : selectedModel.modelName())
                .responseId(response.id())
                .finishReason(response.finishReason() == null ? null : response.finishReason().name())
                .inputTokens(tokenUsage == null ? null : tokenUsage.inputTokenCount())
                .outputTokens(tokenUsage == null ? null : tokenUsage.outputTokenCount())
                .totalTokens(tokenUsage == null ? null : tokenUsage.totalTokenCount())
                .build();
    }

    private SelectedChatModel defaultModel() {
        ChatModel deepseek = chatModelRegistry.get("deepseek-chat");
        if (deepseek != null) {
            return new SelectedChatModel("deepseek-chat", deepseek);
        }
        ChatModel zhipu = chatModelRegistry.get("glm-4.6");
        if (zhipu != null) {
            return new SelectedChatModel("glm-4.6", zhipu);
        }
        return chatModelRegistry.asMap().entrySet().stream()
                .findFirst()
                .map(entry -> new SelectedChatModel(entry.getKey(), entry.getValue()))
                .orElseThrow(() -> new IllegalStateException("No usable LangChain4j ChatModel configured"));
    }

    private record SelectedChatModel(String modelName, ChatModel model) {
    }
}
