package aliang.flowcopilot.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import aliang.flowcopilot.converter.ChatMessageConverter;
import aliang.flowcopilot.event.ChatEvent;
import aliang.flowcopilot.exception.BizException;
import aliang.flowcopilot.mapper.ChatMessageMapper;
import aliang.flowcopilot.model.dto.ChatMessageDTO;
import aliang.flowcopilot.model.entity.ChatMessage;
import aliang.flowcopilot.model.request.CreateChatMessageRequest;
import aliang.flowcopilot.model.request.UpdateChatMessageRequest;
import aliang.flowcopilot.model.response.CreateChatMessageResponse;
import aliang.flowcopilot.model.response.GetChatMessagesResponse;
import aliang.flowcopilot.model.vo.ChatMessageVO;
import aliang.flowcopilot.service.ChatMessageFacadeService;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天消息门面服务实现。
 * <p>
 * 它不仅负责消息的 CRUD，还承担“用户消息触发 Agent 执行”的事件桥梁职责。
 */
@Service
@AllArgsConstructor
public class ChatMessageFacadeServiceImpl implements ChatMessageFacadeService {

    private final ChatMessageMapper chatMessageMapper;
    private final ChatMessageConverter chatMessageConverter;
    private final ApplicationEventPublisher publisher;

    @Override
    /**
     * 查询会话下的全部消息并转为展示对象。
     *
     * @param sessionId 会话 ID
     * @return 消息列表响应
     */
    public GetChatMessagesResponse getChatMessagesBySessionId(String sessionId) {
        List<ChatMessage> chatMessages = chatMessageMapper.selectBySessionId(sessionId);
        List<ChatMessageVO> result = new ArrayList<>();

        for (ChatMessage chatMessage : chatMessages) {
            try {
                ChatMessageVO vo = chatMessageConverter.toVO(chatMessage);
                result.add(vo);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        return GetChatMessagesResponse.builder()
                .chatMessages(result.toArray(new ChatMessageVO[0]))
                .build();
    }

    @Override
    /**
     * 查询会话中最近若干条消息，供 Agent 恢复短期记忆窗口。
     *
     * @param sessionId 会话 ID
     * @param limit 限制返回条数
     * @return DTO 列表
     */
    public List<ChatMessageDTO> getChatMessagesBySessionIdRecently(String sessionId, int limit) {
        List<ChatMessage> chatMessages = chatMessageMapper.selectBySessionIdRecently(sessionId, limit);
        List<ChatMessageDTO> result = new ArrayList<>();
        for (ChatMessage chatMessage : chatMessages) {
            try {
                ChatMessageDTO dto = chatMessageConverter.toDTO(chatMessage);
                result.add(dto);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    @Override
    /**
     * 创建用户消息并发布聊天事件。
     *
     * @param request 创建消息请求
     * @return 新建消息 ID
     */
    public CreateChatMessageResponse createChatMessage(CreateChatMessageRequest request) {
        ChatMessage chatMessage = doCreateChatMessage(request);
        // 发布聊天通知事件
        publisher.publishEvent(new ChatEvent(
                        request.getAgentId(),
                        chatMessage.getSessionId(),
                        chatMessage.getContent()
                )
        );
        // 返回生成的 chatMessageId
        return CreateChatMessageResponse.builder()
                .chatMessageId(chatMessage.getId())
                .build();
    }

    @Override
    /**
     * 仅持久化一条 DTO 形式的聊天消息，不额外触发事件。
     *
     * @param chatMessageDTO 聊天消息 DTO
     * @return 新建消息 ID
     */
    public CreateChatMessageResponse createChatMessage(ChatMessageDTO chatMessageDTO) {
        ChatMessage chatMessage = doCreateChatMessage(chatMessageDTO);
        return CreateChatMessageResponse.builder()
                .chatMessageId(chatMessage.getId())
                .build();
    }

    @Override
    /**
     * 由 Agent 自身创建一条消息。
     * <p>
     * 该方法不会再次发布聊天事件，避免 Agent 输出形成递归执行。
     *
     * @param request 创建请求
     * @return 新建消息 ID
     */
    public CreateChatMessageResponse agentCreateChatMessage(CreateChatMessageRequest request) {
        ChatMessage chatMessage = doCreateChatMessage(request);
        // 和 createChatMessage 的区别在于，Agent 创建的 chatMessage 不需要发布事件
        return CreateChatMessageResponse.builder()
                .chatMessageId(chatMessage.getId())
                .build();
    }

    /**
     * 将外部请求对象转换为实体并完成持久化。
     *
     * @param request 创建请求
     * @return 持久化后的实体
     */
    private ChatMessage doCreateChatMessage(CreateChatMessageRequest request) {
        // 将 CreateChatMessageRequest 转换为 ChatMessageDTO
        ChatMessageDTO chatMessageDTO = chatMessageConverter.toDTO(request);
        // 将 ChatMessageDTO 转换为 ChatMessage 实体
        return doCreateChatMessage(chatMessageDTO);
    }

    /**
     * 将 DTO 转为实体并写入数据库。
     *
     * @param chatMessageDTO 聊天消息 DTO
     * @return 持久化后的消息实体
     */
    private ChatMessage doCreateChatMessage(ChatMessageDTO chatMessageDTO) {
        try {
            // 将 ChatMessageDTO 转换为 ChatMessage 实体
            ChatMessage chatMessage = chatMessageConverter.toEntity(chatMessageDTO);

            // 设置创建时间和更新时间
            LocalDateTime now = LocalDateTime.now();
            chatMessage.setCreatedAt(now);
            chatMessage.setUpdatedAt(now);
            // 插入数据库，ID 由数据库自动生成
            int result = chatMessageMapper.insert(chatMessage);
            if (result <= 0) {
                throw new BizException("创建聊天消息失败");
            }
            return chatMessage;
        } catch (JsonProcessingException e) {
            throw new BizException("创建聊天消息时发生序列化错误: " + e.getMessage());
        }
    }

    @Override
    /**
     * 追加消息内容，适合流式输出或分段补全场景。
     *
     * @param chatMessageId 目标消息 ID
     * @param appendContent 追加内容
     * @return 被更新消息的 ID
     */
    public CreateChatMessageResponse appendChatMessage(String chatMessageId, String appendContent) {
        // 查询现有的聊天消息
        ChatMessage existingChatMessage = chatMessageMapper.selectById(chatMessageId);
        if (existingChatMessage == null) {
            throw new BizException("聊天消息不存在: " + chatMessageId);
        }

        // 将追加内容添加到现有内容后面
        String currentContent = existingChatMessage.getContent() != null
                ? existingChatMessage.getContent()
                : "";
        String updatedContent = currentContent + appendContent;

        // 创建更新后的消息对象
        ChatMessage updatedChatMessage = ChatMessage.builder()
                .id(existingChatMessage.getId())
                .sessionId(existingChatMessage.getSessionId())
                .role(existingChatMessage.getRole())
                .content(updatedContent)
                .metadata(existingChatMessage.getMetadata())
                .createdAt(existingChatMessage.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        // 更新数据库
        int result = chatMessageMapper.updateById(updatedChatMessage);
        if (result <= 0) {
            throw new BizException("追加聊天消息内容失败");
        }

        // 返回聊天消息ID
        return CreateChatMessageResponse.builder()
                .chatMessageId(chatMessageId)
                .build();
    }

    @Override
    /**
     * 删除指定聊天消息。
     *
     * @param chatMessageId 消息 ID
     */
    public void deleteChatMessage(String chatMessageId) {
        ChatMessage chatMessage = chatMessageMapper.selectById(chatMessageId);
        if (chatMessage == null) {
            throw new BizException("聊天消息不存在: " + chatMessageId);
        }

        int result = chatMessageMapper.deleteById(chatMessageId);
        if (result <= 0) {
            throw new BizException("删除聊天消息失败");
        }
    }

    @Override
    /**
     * 更新指定聊天消息。
     *
     * @param chatMessageId 消息 ID
     * @param request 更新请求
     */
    public void updateChatMessage(String chatMessageId, UpdateChatMessageRequest request) {
        try {
            // 查询现有的聊天消息
            ChatMessage existingChatMessage = chatMessageMapper.selectById(chatMessageId);
            if (existingChatMessage == null) {
                throw new BizException("聊天消息不存在: " + chatMessageId);
            }

            // 将现有 ChatMessage 转换为 ChatMessageDTO
            ChatMessageDTO chatMessageDTO = chatMessageConverter.toDTO(existingChatMessage);

            // 使用 UpdateChatMessageRequest 更新 ChatMessageDTO
            chatMessageConverter.updateDTOFromRequest(chatMessageDTO, request);

            // 将更新后的 ChatMessageDTO 转换回 ChatMessage 实体
            ChatMessage updatedChatMessage = chatMessageConverter.toEntity(chatMessageDTO);

            // 保留原有的 ID、sessionId、role 和创建时间
            updatedChatMessage.setId(existingChatMessage.getId());
            updatedChatMessage.setSessionId(existingChatMessage.getSessionId());
            updatedChatMessage.setRole(existingChatMessage.getRole());
            updatedChatMessage.setCreatedAt(existingChatMessage.getCreatedAt());
            updatedChatMessage.setUpdatedAt(LocalDateTime.now());

            // 更新数据库
            int result = chatMessageMapper.updateById(updatedChatMessage);
            if (result <= 0) {
                throw new BizException("更新聊天消息失败");
            }
        } catch (JsonProcessingException e) {
            throw new BizException("更新聊天消息时发生序列化错误: " + e.getMessage());
        }
    }
}
