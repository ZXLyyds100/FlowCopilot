package aliang.flowcopilot.agent;

import aliang.flowcopilot.agent.tools.ToolBinding;
import aliang.flowcopilot.converter.ChatMessageConverter;
import aliang.flowcopilot.message.SseMessage;
import aliang.flowcopilot.model.dto.AgentDTO;
import aliang.flowcopilot.model.dto.ChatMessageDTO;
import aliang.flowcopilot.model.dto.KnowledgeBaseDTO;
import aliang.flowcopilot.model.response.CreateChatMessageResponse;
import aliang.flowcopilot.model.vo.ChatMessageVO;
import aliang.flowcopilot.service.ChatMessageFacadeService;
import aliang.flowcopilot.service.SseService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.tool.ToolExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * JChatMind Agent 杩愯鏃舵牳蹇冩墽琛屽櫒銆?
 */
@Slf4j
public class JChatMind {

    private static final Integer MAX_STEPS = 20;
    private static final Integer DEFAULT_MAX_MESSAGES = 20;

    private final String agentId;
    private final String name;
    private final String description;
    private final String systemPrompt;
    private final ChatModel chatModel;
    private final List<ToolBinding> availableTools;
    private final List<KnowledgeBaseDTO> availableKbs;
    private final String chatSessionId;
    private final SseService sseService;
    private final ChatMessageFacadeService chatMessageFacadeService;
    private final ChatMessageConverter chatMessageConverter;
    private final ChatMemory chatMemory;
    private final AgentDTO.ChatOptions runtimeChatOptions;
    private final List<ChatMessageDTO> pendingChatMessages = new ArrayList<>();

    private AgentState agentState;
    private ChatResponse lastChatResponse;
    private AiMessage lastAiMessage;

    public JChatMind(String agentId,
                     String name,
                     String description,
                     String systemPrompt,
                     ChatModel chatModel,
                     AgentDTO.ChatOptions runtimeChatOptions,
                     Integer maxMessages,
                     List<ChatMessage> memory,
                     List<ToolBinding> availableTools,
                     List<KnowledgeBaseDTO> availableKbs,
                     String chatSessionId,
                     SseService sseService,
                     ChatMessageFacadeService chatMessageFacadeService,
                     ChatMessageConverter chatMessageConverter) {
        this.agentId = agentId;
        this.name = name;
        this.description = description;
        this.systemPrompt = systemPrompt;
        this.chatModel = chatModel;
        this.runtimeChatOptions = runtimeChatOptions == null ? AgentDTO.ChatOptions.defaultOptions() : runtimeChatOptions;
        this.availableTools = availableTools == null ? List.of() : List.copyOf(availableTools);
        this.availableKbs = availableKbs == null ? List.of() : List.copyOf(availableKbs);
        this.chatSessionId = chatSessionId;
        this.sseService = sseService;
        this.chatMessageFacadeService = chatMessageFacadeService;
        this.chatMessageConverter = chatMessageConverter;
        this.agentState = AgentState.IDLE;

        this.chatMemory = MessageWindowChatMemory.builder()
                .id(chatSessionId)
                .maxMessages(maxMessages == null ? DEFAULT_MAX_MESSAGES : maxMessages)
                .build();
        this.chatMemory.add(memory);

        if (StringUtils.hasLength(systemPrompt)) {
            this.chatMemory.add(SystemMessage.from(systemPrompt));
        }
    }

    private void logToolCalls(List<ToolExecutionRequest> toolCalls) {
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

    private void saveMessage(ChatMessage message) {
        ChatMessageDTO.ChatMessageDTOBuilder builder = ChatMessageDTO.builder()
                .sessionId(this.chatSessionId);

        if (message instanceof AiMessage aiMessage) {
            List<ChatMessageDTO.ToolCallPayload> toolCalls = aiMessage.toolExecutionRequests()
                    .stream()
                    .map(this::toToolCallPayload)
                    .toList();

            ChatMessageDTO.MetaData metaData = toolCalls.isEmpty() ? null : ChatMessageDTO.MetaData.builder()
                    .toolCalls(toolCalls)
                    .build();

            ChatMessageDTO chatMessageDTO = builder.role(ChatMessageDTO.RoleType.ASSISTANT)
                    .content(aiMessage.text())
                    .metadata(metaData)
                    .build();
            CreateChatMessageResponse chatMessage = chatMessageFacadeService.createChatMessage(chatMessageDTO);
            chatMessageDTO.setId(chatMessage.getChatMessageId());
            pendingChatMessages.add(chatMessageDTO);
            return;
        }

        if (message instanceof ToolExecutionResultMessage toolResponseMessage) {
            ChatMessageDTO chatMessageDTO = builder.role(ChatMessageDTO.RoleType.TOOL)
                    .content(toolResponseMessage.text())
                    .metadata(ChatMessageDTO.MetaData.builder()
                            .toolResponse(toToolResponsePayload(toolResponseMessage))
                            .build())
                    .build();
            CreateChatMessageResponse chatMessage = chatMessageFacadeService.createChatMessage(chatMessageDTO);
            chatMessageDTO.setId(chatMessage.getChatMessageId());
            pendingChatMessages.add(chatMessageDTO);
            return;
        }

        throw new IllegalArgumentException("Unsupported message type: " + message.getClass().getName());
    }

    private ChatMessageDTO.ToolCallPayload toToolCallPayload(ToolExecutionRequest request) {
        return ChatMessageDTO.ToolCallPayload.builder()
                .id(request.id())
                .name(request.name())
                .arguments(request.arguments())
                .build();
    }

    private ChatMessageDTO.ToolResponsePayload toToolResponsePayload(ToolExecutionResultMessage message) {
        return ChatMessageDTO.ToolResponsePayload.builder()
                .id(message.id())
                .name(message.toolName())
                .responseData(message.text())
                .build();
    }

    private void refreshPendingMessages() {
        for (ChatMessageDTO message : pendingChatMessages) {
            ChatMessageVO vo = chatMessageConverter.toVO(message);
            SseMessage sseMessage = SseMessage.builder()
                    .type(SseMessage.Type.AI_GENERATED_CONTENT)
                    .payload(SseMessage.Payload.builder()
                            .message(vo)
                            .build())
                    .metadata(SseMessage.Metadata.builder()
                            .chatMessageId(message.getId())
                            .build())
                    .build();
            sseService.send(this.chatSessionId, sseMessage);
        }
        pendingChatMessages.clear();
    }

    private boolean think() {
        List<ChatMessage> messages = new ArrayList<>(this.chatMemory.messages());
        messages.add(SystemMessage.from(buildThinkPrompt()));

        ChatRequest.Builder requestBuilder = ChatRequest.builder()
                .messages(messages)
                .temperature(runtimeChatOptions.getTemperature())
                .topP(runtimeChatOptions.getTopP());

        if (!availableTools.isEmpty()) {
            requestBuilder.toolSpecifications(availableTools.stream()
                    .map(ToolBinding::getSpecification)
                    .toList());
        }

        this.lastChatResponse = this.chatModel.chat(requestBuilder.build());
        Assert.notNull(lastChatResponse, "Last chat response cannot be null");

        this.lastAiMessage = lastChatResponse.aiMessage();
        Assert.notNull(lastAiMessage, "Last AI message cannot be null");

        List<ToolExecutionRequest> toolCalls = lastAiMessage.toolExecutionRequests();

        saveMessage(lastAiMessage);
        refreshPendingMessages();
        logToolCalls(toolCalls);

        if (toolCalls == null || toolCalls.isEmpty()) {
            this.chatMemory.add(lastAiMessage);
            return false;
        }
        return true;
    }

    private String buildThinkPrompt() {
        return """
                现在你是一个智能的「决策模块」。
                请根据当前对话上下文，决定下一步的动作。

                【额外信息】
                - 你目前拥有的知识库列表以及描述：%s
                - 如果有缺失的上下文时，优先从知识库中进行搜索
                """.formatted(this.availableKbs);
    }

    private void execute() {
        Assert.notNull(this.lastAiMessage, "Last AI message cannot be null");
        if (!this.lastAiMessage.hasToolExecutionRequests()) {
            return;
        }

        this.chatMemory.add(this.lastAiMessage);

        List<ToolExecutionResultMessage> toolResponses = new ArrayList<>();
        for (ToolExecutionRequest toolCall : this.lastAiMessage.toolExecutionRequests()) {
            ToolExecutionResultMessage response = executeTool(toolCall);
            toolResponses.add(response);
            this.chatMemory.add(response);
            saveMessage(response);
        }

        refreshPendingMessages();

        if (toolResponses.stream().anyMatch(resp -> "terminate".equals(resp.toolName()))) {
            this.agentState = AgentState.FINISHED;
            log.info("任务结束");
        }
    }

    private ToolExecutionResultMessage executeTool(ToolExecutionRequest toolCall) {
        ToolBinding binding = availableTools.stream()
                .filter(tool -> Objects.equals(tool.getName(), toolCall.name()))
                .findFirst()
                .orElse(null);

        if (binding == null) {
            log.warn("Tool not found: {}", toolCall.name());
            return ToolExecutionResultMessage.builder()
                    .id(defaultToolCallId(toolCall))
                    .toolName(toolCall.name())
                    .text("Tool not found: " + toolCall.name())
                    .isError(true)
                    .build();
        }

        try {
            String result = binding.getExecutor().execute(toolCall, this.chatSessionId);
            log.info("工具 {} 执行完成", toolCall.name());
            return ToolExecutionResultMessage.from(toolCall, result == null ? "" : result);
        } catch (Exception ex) {
            log.error("Tool execution failed: {}", toolCall.name(), ex);
            return ToolExecutionResultMessage.builder()
                    .id(defaultToolCallId(toolCall))
                    .toolName(toolCall.name())
                    .text("Tool execution failed: " + ex.getMessage())
                    .isError(true)
                    .build();
        }
    }

    private String defaultToolCallId(ToolExecutionRequest toolCall) {
        return StringUtils.hasText(toolCall.id()) ? toolCall.id() : UUID.randomUUID().toString();
    }

    private void step() {
        if (think()) {
            execute();
        } else {
            agentState = AgentState.FINISHED;
        }
    }

    public void run() {
        if (agentState != AgentState.IDLE) {
            throw new IllegalStateException("Agent is not idle");
        }

        try {
            for (int i = 0; i < MAX_STEPS && agentState != AgentState.FINISHED; i++) {
                int currentStep = i + 1;
                step();
                if (currentStep >= MAX_STEPS) {
                    agentState = AgentState.FINISHED;
                    log.warn("Max steps reached, stopping agent");
                }
            }
            agentState = AgentState.FINISHED;
        } catch (Exception e) {
            agentState = AgentState.ERROR;
            log.error("Error running agent", e);
            throw new RuntimeException("Error running agent", e);
        }
    }

    @Override
    public String toString() {
        return "JChatMind {" +
                "name = " + name + ",\n" +
                "description = " + description + ",\n" +
                "agentId = " + agentId + ",\n" +
                "systemPrompt = " + systemPrompt + "}";
    }
}
