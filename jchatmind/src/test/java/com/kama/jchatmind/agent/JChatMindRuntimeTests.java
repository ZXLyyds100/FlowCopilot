package com.kama.jchatmind.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kama.jchatmind.agent.tools.ToolBinding;
import com.kama.jchatmind.converter.ChatMessageConverter;
import com.kama.jchatmind.model.dto.AgentDTO;
import com.kama.jchatmind.model.dto.ChatMessageDTO;
import com.kama.jchatmind.model.dto.KnowledgeBaseDTO;
import com.kama.jchatmind.model.response.CreateChatMessageResponse;
import com.kama.jchatmind.service.ChatMessageFacadeService;
import com.kama.jchatmind.service.SseService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JChatMindRuntimeTests {

    @Test
    void shouldRunToolLoopAndPersistMessages() throws Exception {
        RecordingChatModel chatModel = new RecordingChatModel();
        chatModel.enqueue(request -> ChatResponse.builder()
                .aiMessage(AiMessage.builder()
                        .text("")
                        .toolExecutionRequests(List.of(ToolExecutionRequest.builder()
                                .id("call-1")
                                .name("weather")
                                .arguments("{\"city\":\"Shanghai\"}")
                                .build()))
                        .build())
                .build());
        chatModel.enqueue(request -> {
            boolean hasToolResult = request.messages().stream()
                    .anyMatch(message -> message instanceof ToolExecutionResultMessage toolMessage
                            && "weather".equals(toolMessage.toolName())
                            && toolMessage.text().contains("Sunny"));
            assertTrue(hasToolResult);
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from("Today in Shanghai it is sunny, 25C"))
                    .build();
        });

        WeatherLookupTool tool = new WeatherLookupTool();
        ToolBinding binding = buildBinding(tool, "weather", String.class);

        ChatMessageFacadeService chatMessageFacadeService = mock(ChatMessageFacadeService.class);
        SseService sseService = mock(SseService.class);
        AtomicInteger ids = new AtomicInteger();
        when(chatMessageFacadeService.createChatMessage(any(ChatMessageDTO.class))).thenAnswer(invocation ->
                CreateChatMessageResponse.builder()
                        .chatMessageId("msg-" + ids.incrementAndGet())
                        .build()
        );

        JChatMind agent = new JChatMind(
                "agent-1",
                "Weather Agent",
                "Weather helper",
                "You are a weather assistant.",
                chatModel,
                AgentDTO.ChatOptions.builder().temperature(0.3).topP(0.7).messageLength(10).build(),
                10,
                List.<ChatMessage>of(),
                List.of(binding),
                List.<KnowledgeBaseDTO>of(),
                "session-1",
                sseService,
                chatMessageFacadeService,
                new ChatMessageConverter(new ObjectMapper())
        );

        agent.run();

        assertEquals(2, chatModel.requests().size());

        ArgumentCaptor<ChatMessageDTO> captor = ArgumentCaptor.forClass(ChatMessageDTO.class);
        verify(chatMessageFacadeService, times(3)).createChatMessage(captor.capture());
        verify(sseService, times(3)).send(eq("session-1"), any());

        List<ChatMessageDTO> savedMessages = captor.getAllValues();
        assertEquals(ChatMessageDTO.RoleType.ASSISTANT, savedMessages.get(0).getRole());
        assertNotNull(savedMessages.get(0).getMetadata());
        assertEquals(1, savedMessages.get(0).getMetadata().getToolCalls().size());
        assertEquals("weather", savedMessages.get(0).getMetadata().getToolCalls().get(0).getName());

        assertEquals(ChatMessageDTO.RoleType.TOOL, savedMessages.get(1).getRole());
        assertNotNull(savedMessages.get(1).getMetadata().getToolResponse());
        assertEquals("weather", savedMessages.get(1).getMetadata().getToolResponse().getName());
        assertTrue(savedMessages.get(1).getContent().contains("Sunny"));

        assertEquals(ChatMessageDTO.RoleType.ASSISTANT, savedMessages.get(2).getRole());
        assertEquals("Today in Shanghai it is sunny, 25C", savedMessages.get(2).getContent());
    }

    @Test
    void shouldPassTemperatureAndTopPToChatRequest() {
        RecordingChatModel chatModel = new RecordingChatModel();
        chatModel.enqueue(request -> ChatResponse.builder()
                .aiMessage(AiMessage.from("hello"))
                .build());

        ChatMessageFacadeService chatMessageFacadeService = mock(ChatMessageFacadeService.class);
        when(chatMessageFacadeService.createChatMessage(any(ChatMessageDTO.class))).thenReturn(
                CreateChatMessageResponse.builder().chatMessageId("msg-1").build()
        );

        JChatMind agent = new JChatMind(
                "agent-1",
                "Simple Agent",
                "Simple helper",
                "You are a simple assistant.",
                chatModel,
                AgentDTO.ChatOptions.builder().temperature(0.25).topP(0.66).messageLength(10).build(),
                10,
                List.<ChatMessage>of(),
                List.<ToolBinding>of(),
                List.<KnowledgeBaseDTO>of(),
                "session-2",
                mock(SseService.class),
                chatMessageFacadeService,
                new ChatMessageConverter(new ObjectMapper())
        );

        agent.run();

        ChatRequest capturedRequest = chatModel.requests().get(0);
        assertEquals(0.25, capturedRequest.temperature());
        assertEquals(0.66, capturedRequest.topP());
        assertFalse(capturedRequest.messages().isEmpty());
    }

    private ToolBinding buildBinding(Object tool, String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = tool.getClass().getMethod(methodName, parameterTypes);
        ToolSpecification specification = ToolSpecifications.toolSpecificationFrom(method);
        return ToolBinding.builder()
                .name(specification.name())
                .specification(specification)
                .executor(DefaultToolExecutor.builder()
                        .object(tool)
                        .methodToInvoke(method)
                        .originalMethod(method)
                        .build())
                .build();
    }

    private static final class RecordingChatModel implements ChatModel {
        private final Queue<Function<ChatRequest, ChatResponse>> responses = new ArrayDeque<>();
        private final List<ChatRequest> requests = new ArrayList<>();

        void enqueue(Function<ChatRequest, ChatResponse> responseFactory) {
            responses.add(responseFactory);
        }

        List<ChatRequest> requests() {
            return requests;
        }

        @Override
        public ChatResponse chat(ChatRequest request) {
            requests.add(request);
            Function<ChatRequest, ChatResponse> responseFactory = responses.poll();
            if (responseFactory == null) {
                throw new IllegalStateException("No response configured for request");
            }
            return responseFactory.apply(request);
        }
    }

    private static final class WeatherLookupTool {

        @dev.langchain4j.agent.tool.Tool(name = "weather", value = "Lookup weather by city")
        public String weather(@P("city") String city) {
            return city + ": Sunny, 25C";
        }
    }
}
