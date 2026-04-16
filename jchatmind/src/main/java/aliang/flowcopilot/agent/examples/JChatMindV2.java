package aliang.flowcopilot.agent.examples;

import aliang.flowcopilot.agent.AgentState;
import aliang.flowcopilot.agent.tools.ToolBinding;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * V2 鐗堟湰锛氭坊鍔犲伐鍏疯皟鐢ㄧ殑绀轰緥銆?
 */
@Slf4j
public class JChatMindV2 extends JChatMindV1 {

    protected List<ToolBinding> availableTools;
    protected ChatResponse lastChatResponse;
    protected AiMessage lastAiMessage;

    private static final Integer MAX_STEPS = 20;

    public JChatMindV2() {
        super();
    }

    public JChatMindV2(String name,
                       String description,
                       String systemPrompt,
                       ChatModel chatModel,
                       Integer maxMessages,
                       String sessionId,
                       List<ToolBinding> availableTools) {
        super(name, description, systemPrompt, chatModel, maxMessages, sessionId);
        this.availableTools = availableTools == null ? List.of() : List.copyOf(availableTools);
    }

    protected void logToolCalls(List<ToolExecutionRequest> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            log.info("\n\n[ToolCalling] 无工具调用");
            return;
        }
        String logMessage = IntStream.range(0, toolCalls.size())
                .mapToObj(i -> {
                    ToolExecutionRequest call = toolCalls.get(i);
                    return String.format(
                            "[ToolCalling #%d]\n- name      : %s\n- arguments : %s",
                            i + 1,
                            call.name(),
                            call.arguments()
                    );
                })
                .collect(Collectors.joining("\n\n"));
        log.info("\n\n========== Tool Calling ==========\n{}\n=================================\n", logMessage);
    }

    protected boolean think() {
        ChatRequest.Builder requestBuilder = ChatRequest.builder()
                .messages(chatMemory.messages());

        if (!availableTools.isEmpty()) {
            requestBuilder.toolSpecifications(availableTools.stream()
                    .map(ToolBinding::getSpecification)
                    .toList());
        }

        lastChatResponse = chatModel.chat(requestBuilder.build());
        Assert.notNull(lastChatResponse, "Last chat response cannot be null");

        lastAiMessage = lastChatResponse.aiMessage();
        Assert.notNull(lastAiMessage, "Last AI message cannot be null");

        List<ToolExecutionRequest> toolCalls = lastAiMessage.toolExecutionRequests();
        logToolCalls(toolCalls);

        if (toolCalls == null || toolCalls.isEmpty()) {
            chatMemory.add(lastAiMessage);
            return false;
        }
        return true;
    }

    protected void execute() {
        Assert.notNull(lastAiMessage, "Last AI message cannot be null");
        if (!lastAiMessage.hasToolExecutionRequests()) {
            return;
        }

        chatMemory.add(lastAiMessage);
        List<ToolExecutionResultMessage> responses = new ArrayList<>();
        for (ToolExecutionRequest request : lastAiMessage.toolExecutionRequests()) {
            ToolBinding binding = availableTools.stream()
                    .filter(tool -> Objects.equals(tool.getName(), request.name()))
                    .findFirst()
                    .orElse(null);

            ToolExecutionResultMessage response;
            if (binding == null) {
                response = ToolExecutionResultMessage.builder()
                        .id(request.id() == null ? UUID.randomUUID().toString() : request.id())
                        .toolName(request.name())
                        .text("Tool not found: " + request.name())
                        .isError(true)
                        .build();
            } else {
                String result = binding.getExecutor().execute(request, sessionId);
                response = ToolExecutionResultMessage.from(request, result == null ? "" : result);
            }
            responses.add(response);
            chatMemory.add(response);
        }

        if (responses.stream().anyMatch(resp -> "terminate".equals(resp.toolName()))) {
            agentState = AgentState.FINISHED;
        }
    }

    protected void step() {
        if (think()) {
            execute();
        } else {
            agentState = AgentState.FINISHED;
        }
    }

    @Override
    public String chat(String userInput) {
        Assert.notNull(userInput, "鐢ㄦ埛杈撳叆涓嶈兘涓虹┖");
        if (agentState != AgentState.IDLE) {
            throw new IllegalStateException("Agent 鐘舵€佷笉鏄?IDLE锛屽綋鍓嶇姸鎬侊細" + agentState);
        }

        try {
            agentState = AgentState.THINKING;
            chatMemory.add(UserMessage.from(userInput));

            for (int i = 0; i < MAX_STEPS && agentState != AgentState.FINISHED; i++) {
                step();
                if (i >= MAX_STEPS - 1) {
                    agentState = AgentState.FINISHED;
                    log.warn("杈惧埌鏈€澶ф楠ゆ暟锛屽仠姝?Agent");
                }
            }

            List<ChatMessage> history = chatMemory.messages();
            for (int i = history.size() - 1; i >= 0; i--) {
                ChatMessage message = history.get(i);
                if (message instanceof AiMessage aiMessage && !aiMessage.hasToolExecutionRequests()) {
                    return aiMessage.text();
                }
            }
            return "";
        } catch (Exception e) {
            agentState = AgentState.ERROR;
            log.error("Agent 杩愯杩囩▼涓彂鐢熼敊璇?", e);
            throw new RuntimeException("Agent 杩愯杩囩▼涓彂鐢熼敊璇?", e);
        } finally {
            agentState = AgentState.IDLE;
        }
    }
}
