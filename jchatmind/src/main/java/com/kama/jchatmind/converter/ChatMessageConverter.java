package com.kama.jchatmind.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kama.jchatmind.model.dto.ChatMessageDTO;
import com.kama.jchatmind.model.entity.ChatMessage;
import com.kama.jchatmind.model.request.CreateChatMessageRequest;
import com.kama.jchatmind.model.request.UpdateChatMessageRequest;
import com.kama.jchatmind.model.vo.ChatMessageVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * 聊天消息对象转换器。
 * <p>
 * 负责数据库实体、运行时 DTO、前端 VO 以及接口请求对象之间的互转。
 */
@Component
@AllArgsConstructor
public class ChatMessageConverter {

    private final ObjectMapper objectMapper;

    /**
     * 将消息 DTO 转为数据库实体。
     *
     * @param chatMessageDTO 消息 DTO
     * @return 消息实体
     * @throws JsonProcessingException 元数据序列化失败时抛出
     */
    public ChatMessage toEntity(ChatMessageDTO chatMessageDTO) throws JsonProcessingException {
        Assert.notNull(chatMessageDTO, "ChatMessageDTO cannot be null");
        Assert.notNull(chatMessageDTO.getRole(), "Role cannot be null");

        return ChatMessage.builder()
                .id(chatMessageDTO.getId())
                .sessionId(chatMessageDTO.getSessionId())
                .role(chatMessageDTO.getRole().getRole())
                .content(chatMessageDTO.getContent())
                .metadata(chatMessageDTO.getMetadata() != null
                        ? objectMapper.writeValueAsString(chatMessageDTO.getMetadata())
                        : null)
                .createdAt(chatMessageDTO.getCreatedAt())
                .updatedAt(chatMessageDTO.getUpdatedAt())
                .build();
    }

    /**
     * 将数据库实体转为消息 DTO。
     *
     * @param chatMessage 消息实体
     * @return 消息 DTO
     * @throws JsonProcessingException 元数据反序列化失败时抛出
     */
    public ChatMessageDTO toDTO(ChatMessage chatMessage) throws JsonProcessingException {
        Assert.notNull(chatMessage, "ChatMessage cannot be null");
        Assert.notNull(chatMessage.getRole(), "Role cannot be null");

        return ChatMessageDTO.builder()
                .id(chatMessage.getId())
                .sessionId(chatMessage.getSessionId())
                .role(ChatMessageDTO.RoleType.fromRole(chatMessage.getRole()))
                .content(chatMessage.getContent())
                .metadata(chatMessage.getMetadata() != null
                        ? objectMapper.readValue(chatMessage.getMetadata(), ChatMessageDTO.MetaData.class)
                        : null)
                .createdAt(chatMessage.getCreatedAt())
                .updatedAt(chatMessage.getUpdatedAt())
                .build();
    }

    /**
     * 将消息 DTO 转为前端展示对象。
     *
     * @param dto 消息 DTO
     * @return 消息 VO
     */
    public ChatMessageVO toVO(ChatMessageDTO dto) {
        return ChatMessageVO.builder()
                .id(dto.getId())
                .sessionId(dto.getSessionId())
                .role(dto.getRole())
                .content(dto.getContent())
                .metadata(dto.getMetadata())
                .build();
    }

    /**
     * 将消息实体直接转为前端展示对象。
     *
     * @param chatMessage 消息实体
     * @return 消息 VO
     * @throws JsonProcessingException JSON 转换失败时抛出
     */
    public ChatMessageVO toVO(ChatMessage chatMessage) throws JsonProcessingException {
        return toVO(toDTO(chatMessage));
    }

    /**
     * 将创建消息请求转换为消息 DTO。
     *
     * @param request 创建请求
     * @return 消息 DTO
     */
    public ChatMessageDTO toDTO(CreateChatMessageRequest request) {
        Assert.notNull(request, "CreateChatMessageRequest cannot be null");
        Assert.notNull(request.getSessionId(), "SessionId cannot be null");
        Assert.notNull(request.getRole(), "Role cannot be null");

        return ChatMessageDTO.builder()
                .sessionId(request.getSessionId())
                .role(request.getRole())
                .content(request.getContent())
                .metadata(request.getMetadata())
                .build();
    }

    /**
     * 用更新请求中的非空字段覆盖 DTO。
     *
     * @param dto 目标 DTO
     * @param request 更新请求
     */
    public void updateDTOFromRequest(ChatMessageDTO dto, UpdateChatMessageRequest request) {
        Assert.notNull(dto, "ChatMessageDTO cannot be null");
        Assert.notNull(request, "UpdateChatMessageRequest cannot be null");

        if (request.getContent() != null) {
            dto.setContent(request.getContent());
        }
        if (request.getMetadata() != null) {
            dto.setMetadata(request.getMetadata());
        }
    }
}
