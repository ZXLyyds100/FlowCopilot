package aliang.flowcopilot.model.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import aliang.flowcopilot.converter.ChatMessageConverter;
import aliang.flowcopilot.model.entity.ChatMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChatMessageMetadataCompatibilityTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeLegacyMetadataShape() throws Exception {
        String legacyMetadata = """
                {
                  "toolCalls": [
                    {
                      "id": "call-1",
                      "type": "function",
                      "name": "KnowledgeTool",
                      "arguments": "{\\"query\\":\\"hello\\"}"
                    }
                  ],
                  "toolResponse": {
                    "id": "call-1",
                    "toolName": "KnowledgeTool",
                    "text": "knowledge result"
                  }
                }
                """;

        ChatMessageDTO.MetaData metadata = objectMapper.readValue(legacyMetadata, ChatMessageDTO.MetaData.class);

        assertNotNull(metadata);
        assertNotNull(metadata.getToolCalls());
        assertEquals(1, metadata.getToolCalls().size());
        assertEquals("call-1", metadata.getToolCalls().get(0).getId());
        assertEquals("KnowledgeTool", metadata.getToolCalls().get(0).getName());
        assertEquals("{\"query\":\"hello\"}", metadata.getToolCalls().get(0).getArguments());
        assertNotNull(metadata.getToolResponse());
        assertEquals("call-1", metadata.getToolResponse().getId());
        assertEquals("KnowledgeTool", metadata.getToolResponse().getName());
        assertEquals("knowledge result", metadata.getToolResponse().getResponseData());
    }

    @Test
    void converterShouldReadLegacyEntityMetadata() throws Exception {
        ChatMessage entity = ChatMessage.builder()
                .id("msg-1")
                .sessionId("session-1")
                .role("tool")
                .content("knowledge result")
                .metadata("""
                        {
                          "toolResponse": {
                            "id": "call-1",
                            "toolName": "KnowledgeTool",
                            "text": "knowledge result"
                          }
                        }
                        """)
                .build();

        ChatMessageDTO dto = new ChatMessageConverter(objectMapper).toDTO(entity);

        assertNotNull(dto.getMetadata());
        assertNotNull(dto.getMetadata().getToolResponse());
        assertEquals("call-1", dto.getMetadata().getToolResponse().getId());
        assertEquals("KnowledgeTool", dto.getMetadata().getToolResponse().getName());
        assertEquals("knowledge result", dto.getMetadata().getToolResponse().getResponseData());
    }
}
