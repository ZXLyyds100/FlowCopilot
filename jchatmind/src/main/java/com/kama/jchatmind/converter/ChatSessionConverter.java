package com.kama.jchatmind.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kama.jchatmind.model.dto.ChatSessionDTO;
import com.kama.jchatmind.model.entity.ChatSession;
import com.kama.jchatmind.model.request.CreateChatSessionRequest;
import com.kama.jchatmind.model.request.UpdateChatSessionRequest;
import com.kama.jchatmind.model.vo.ChatSessionVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * 聊天会话对象转换器。
 * <p>
 * 负责会话在 Request、DTO、Entity、VO 之间的转换，并处理元数据 JSON 字段。
 */
@Component
@AllArgsConstructor
public class ChatSessionConverter {

    private final ObjectMapper objectMapper;

    /**
     * 将会话 DTO 转换为数据库实体。
     *
     * @param chatSessionDTO 会话 DTO
     * @return 会话实体
     * @throws JsonProcessingException 元数据序列化失败时抛出
     */
    public ChatSession toEntity(ChatSessionDTO chatSessionDTO) throws JsonProcessingException {
        Assert.notNull(chatSessionDTO, "ChatSessionDTO cannot be null");

        return ChatSession.builder()
                .id(chatSessionDTO.getId())
                .agentId(chatSessionDTO.getAgentId())
                .title(chatSessionDTO.getTitle())
                .metadata(chatSessionDTO.getMetadata() != null 
                        ? objectMapper.writeValueAsString(chatSessionDTO.getMetadata()) 
                        : null)
                .createdAt(chatSessionDTO.getCreatedAt())
                .updatedAt(chatSessionDTO.getUpdatedAt())
                .build();
    }

    /**
     * 将会话实体转换为 DTO。
     *
     * @param chatSession 会话实体
     * @return 会话 DTO
     * @throws JsonProcessingException 元数据反序列化失败时抛出
     */
    public ChatSessionDTO toDTO(ChatSession chatSession) throws JsonProcessingException {
        Assert.notNull(chatSession, "ChatSession cannot be null");

        return ChatSessionDTO.builder()
                .id(chatSession.getId())
                .agentId(chatSession.getAgentId())
                .title(chatSession.getTitle())
                .metadata(chatSession.getMetadata() != null 
                        ? objectMapper.readValue(chatSession.getMetadata(), ChatSessionDTO.MetaData.class) 
                        : null)
                .createdAt(chatSession.getCreatedAt())
                .updatedAt(chatSession.getUpdatedAt())
                .build();
    }

    /**
     * 将会话 DTO 转为前端展示对象。
     *
     * @param dto 会话 DTO
     * @return 会话 VO
     */
    public ChatSessionVO toVO(ChatSessionDTO dto) {
        return ChatSessionVO.builder()
                .id(dto.getId())
                .agentId(dto.getAgentId())
                .title(dto.getTitle())
                .build();
    }

    /**
     * 将会话实体直接转为前端展示对象。
     *
     * @param chatSession 会话实体
     * @return 会话 VO
     * @throws JsonProcessingException JSON 转换失败时抛出
     */
    public ChatSessionVO toVO(ChatSession chatSession) throws JsonProcessingException {
        return toVO(toDTO(chatSession));
    }

    /**
     * 将创建会话请求转换为 DTO。
     *
     * @param request 创建请求
     * @return 会话 DTO
     */
    public ChatSessionDTO toDTO(CreateChatSessionRequest request) {
        Assert.notNull(request, "CreateChatSessionRequest cannot be null");
        Assert.notNull(request.getAgentId(), "AgentId cannot be null");

        return ChatSessionDTO.builder()
                .agentId(request.getAgentId())
                .title(request.getTitle())
                .build();
    }

    /**
     * 用更新请求中的非空字段覆盖 DTO。
     *
     * @param dto 目标 DTO
     * @param request 更新请求
     */
    public void updateDTOFromRequest(ChatSessionDTO dto, UpdateChatSessionRequest request) {
        Assert.notNull(dto, "ChatSessionDTO cannot be null");
        Assert.notNull(request, "UpdateChatSessionRequest cannot be null");

        if (request.getTitle() != null) {
            dto.setTitle(request.getTitle());
        }
    }
}
