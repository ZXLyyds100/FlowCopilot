package aliang.flowcopilot.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import aliang.flowcopilot.agent.tools.Tool;
import aliang.flowcopilot.agent.tools.ToolBinding;
import aliang.flowcopilot.config.ChatModelRegistry;
import aliang.flowcopilot.converter.AgentConverter;
import aliang.flowcopilot.converter.ChatMessageConverter;
import aliang.flowcopilot.converter.KnowledgeBaseConverter;
import aliang.flowcopilot.mapper.AgentMapper;
import aliang.flowcopilot.mapper.KnowledgeBaseMapper;
import aliang.flowcopilot.model.dto.AgentDTO;
import aliang.flowcopilot.model.dto.ChatMessageDTO;
import aliang.flowcopilot.model.dto.KnowledgeBaseDTO;
import aliang.flowcopilot.model.entity.Agent;
import aliang.flowcopilot.model.entity.KnowledgeBase;
import aliang.flowcopilot.service.ChatMessageFacadeService;
import aliang.flowcopilot.service.SseService;
import aliang.flowcopilot.service.ToolFacadeService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JChatMindFactory {

    private final ChatModelRegistry chatModelRegistry;
    private final SseService sseService;
    private final AgentMapper agentMapper;
    private final AgentConverter agentConverter;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeBaseConverter knowledgeBaseConverter;
    private final ToolFacadeService toolFacadeService;
    private final ChatMessageFacadeService chatMessageFacadeService;
    private final ChatMessageConverter chatMessageConverter;

    private AgentDTO agentConfig;

    public JChatMindFactory(ChatModelRegistry chatModelRegistry,
                            SseService sseService,
                            AgentMapper agentMapper,
                            AgentConverter agentConverter,
                            KnowledgeBaseMapper knowledgeBaseMapper,
                            KnowledgeBaseConverter knowledgeBaseConverter,
                            ToolFacadeService toolFacadeService,
                            ChatMessageFacadeService chatMessageFacadeService,
                            ChatMessageConverter chatMessageConverter) {
        this.chatModelRegistry = chatModelRegistry;
        this.sseService = sseService;
        this.agentMapper = agentMapper;
        this.agentConverter = agentConverter;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeBaseConverter = knowledgeBaseConverter;
        this.toolFacadeService = toolFacadeService;
        this.chatMessageFacadeService = chatMessageFacadeService;
        this.chatMessageConverter = chatMessageConverter;
    }

    private Agent loadAgent(String agentId) {
        return agentMapper.selectById(agentId);
    }

    private List<ChatMessage> loadMemory(String chatSessionId) {
        int messageLength = agentConfig.getChatOptions().getMessageLength();
        List<ChatMessageDTO> chatMessages =
                chatMessageFacadeService.getChatMessagesBySessionIdRecently(chatSessionId, messageLength);

        List<ChatMessage> memory = new ArrayList<>();
        for (ChatMessageDTO chatMessageDTO : chatMessages) {
            switch (chatMessageDTO.getRole()) {
                case SYSTEM -> {
                    if (!StringUtils.hasLength(chatMessageDTO.getContent())) {
                        continue;
                    }
                    memory.add(0, SystemMessage.from(chatMessageDTO.getContent()));
                }
                case USER -> {
                    if (!StringUtils.hasLength(chatMessageDTO.getContent())) {
                        continue;
                    }
                    memory.add(UserMessage.from(chatMessageDTO.getContent()));
                }
                case ASSISTANT -> memory.add(toAiMessage(chatMessageDTO));
                case TOOL -> memory.add(toToolExecutionResultMessage(chatMessageDTO));
                default -> throw new IllegalStateException("Unsupported message type");
            }
        }
        return memory;
    }

    private AiMessage toAiMessage(ChatMessageDTO chatMessageDTO) {
        List<ToolExecutionRequest> toolCalls = OptionalToolCalls.of(chatMessageDTO.getMetadata())
                .stream()
                .map(toolCall -> ToolExecutionRequest.builder()
                        .id(defaultId(toolCall.getId()))
                        .name(toolCall.getName())
                        .arguments(toolCall.getArguments())
                        .build())
                .toList();

        return AiMessage.builder()
                .text(chatMessageDTO.getContent())
                .toolExecutionRequests(toolCalls)
                .build();
    }

    private ToolExecutionResultMessage toToolExecutionResultMessage(ChatMessageDTO chatMessageDTO) {
        ChatMessageDTO.ToolResponsePayload payload = chatMessageDTO.getMetadata() == null
                ? null
                : chatMessageDTO.getMetadata().getToolResponse();

        if (payload == null) {
            throw new IllegalStateException("Tool message metadata cannot be null");
        }

        return ToolExecutionResultMessage.builder()
                .id(defaultId(payload.getId()))
                .toolName(payload.getName())
                .text(payload.getResponseData())
                .build();
    }

    private AgentDTO toAgentConfig(Agent agent) {
        try {
            agentConfig = agentConverter.toDTO(agent);
            return agentConfig;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("解析 Agent 配置失败", e);
        }
    }

    private List<KnowledgeBaseDTO> resolveRuntimeKnowledgeBases(AgentDTO agentConfig) {
        List<String> allowedKbIds = agentConfig.getAllowedKbs();
        if (allowedKbIds == null || allowedKbIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<KnowledgeBase> knowledgeBases = knowledgeBaseMapper.selectByIdBatch(allowedKbIds);
        if (knowledgeBases.isEmpty()) {
            return Collections.emptyList();
        }

        List<KnowledgeBaseDTO> kbDTOs = new ArrayList<>();
        try {
            for (KnowledgeBase knowledgeBase : knowledgeBases) {
                kbDTOs.add(knowledgeBaseConverter.toDTO(knowledgeBase));
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("解析知识库配置失败", e);
        }
        return kbDTOs;
    }

    private List<Tool> resolveRuntimeTools(AgentDTO agentConfig) {
        List<Tool> runtimeTools = new ArrayList<>(toolFacadeService.getFixedTools());

        List<String> allowedToolNames = agentConfig.getAllowedTools();
        if (allowedToolNames == null || allowedToolNames.isEmpty()) {
            return runtimeTools;
        }

        Map<String, Tool> optionalToolMap = toolFacadeService.getOptionalTools()
                .stream()
                .collect(Collectors.toMap(Tool::getName, Function.identity()));

        for (String toolName : allowedToolNames) {
            Tool tool = optionalToolMap.get(toolName);
            if (tool != null) {
                runtimeTools.add(tool);
            }
        }
        return runtimeTools;
    }

    private List<ToolBinding> buildToolBindings(List<Tool> runtimeTools) {
        List<ToolBinding> bindings = new ArrayList<>();
        for (Tool tool : runtimeTools) {
            Object target = resolveToolTarget(tool);
            for (Method method : target.getClass().getMethods()) {
                dev.langchain4j.agent.tool.Tool annotation =
                        AnnotationUtils.findAnnotation(method, dev.langchain4j.agent.tool.Tool.class);
                if (annotation == null) {
                    continue;
                }
                ToolSpecification specification = ToolSpecifications.toolSpecificationFrom(method);
                DefaultToolExecutor executor = DefaultToolExecutor.builder()
                        .object(target)
                        .methodToInvoke(method)
                        .originalMethod(method)
                        .build();
                bindings.add(ToolBinding.builder()
                        .name(specification.name())
                        .specification(specification)
                        .executor(executor)
                        .build());
            }
        }
        return bindings;
    }

    private Object resolveToolTarget(Tool tool) {
        Object target = AopProxyUtils.getSingletonTarget(tool);
        return target == null ? tool : target;
    }

    private JChatMind buildAgentRuntime(Agent agent,
                                        List<ChatMessage> memory,
                                        List<KnowledgeBaseDTO> knowledgeBases,
                                        List<ToolBinding> toolBindings,
                                        String chatSessionId) {
        ChatModel chatModel = chatModelRegistry.get(agent.getModel());
        if (Objects.isNull(chatModel)) {
            throw new IllegalStateException("""
                    未找到对应的 ChatModel: %s。
                    请检查是否已经完成 LangChain4j provider 配置，并确认对应的 enabled 和 api-key 已正确设置。
                    当前配置前缀为 flowcopilot.llm.providers.*。
                    """.formatted(agent.getModel()).trim());
        }
        return new JChatMind(
                agent.getId(),
                agent.getName(),
                agent.getDescription(),
                agent.getSystemPrompt(),
                chatModel,
                agentConfig.getChatOptions(),
                agentConfig.getChatOptions().getMessageLength(),
                memory,
                toolBindings,
                knowledgeBases,
                chatSessionId,
                sseService,
                chatMessageFacadeService,
                chatMessageConverter
        );
    }

    public JChatMind create(String agentId, String chatSessionId) {
        Agent agent = loadAgent(agentId);
        AgentDTO agentConfig = toAgentConfig(agent);
        List<ChatMessage> memory = loadMemory(chatSessionId);
        List<KnowledgeBaseDTO> knowledgeBases = resolveRuntimeKnowledgeBases(agentConfig);
        List<Tool> runtimeTools = resolveRuntimeTools(agentConfig);
        List<ToolBinding> toolBindings = buildToolBindings(runtimeTools);

        return buildAgentRuntime(agent, memory, knowledgeBases, toolBindings, chatSessionId);
    }

    private String defaultId(String id) {
        return StringUtils.hasText(id) ? id : UUID.randomUUID().toString();
    }

    private static final class OptionalToolCalls {
        private OptionalToolCalls() {
        }

        static List<ChatMessageDTO.ToolCallPayload> of(ChatMessageDTO.MetaData metaData) {
            if (metaData == null || metaData.getToolCalls() == null) {
                return List.of();
            }
            return metaData.getToolCalls();
        }
    }
}
