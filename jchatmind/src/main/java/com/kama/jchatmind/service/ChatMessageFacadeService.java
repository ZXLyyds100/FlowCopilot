package com.kama.jchatmind.service;

import com.kama.jchatmind.model.dto.ChatMessageDTO;
import com.kama.jchatmind.model.request.CreateChatMessageRequest;
import com.kama.jchatmind.model.request.UpdateChatMessageRequest;
import com.kama.jchatmind.model.response.CreateChatMessageResponse;
import com.kama.jchatmind.model.response.GetChatMessagesResponse;

import java.util.List;

/**
 * 聊天消息门面服务。
 * <p>
 * 负责消息的查询、创建、更新、删除，以及 Agent 运行过程中
 * 对消息历史和增量输出的持久化操作。
 */
public interface ChatMessageFacadeService {
    /**
     * 按会话查询完整的聊天消息列表。
     *
     * @param sessionId 会话 ID
     * @return 面向前端展示的聊天消息集合
     */
    GetChatMessagesResponse getChatMessagesBySessionId(String sessionId);

    /**
     * 查询最近若干条聊天消息，用于恢复 Agent 的短期记忆窗口。
     *
     * @param sessionId 会话 ID
     * @param limit 返回消息条数上限
     * @return 运行时使用的消息 DTO 列表
     */
    List<ChatMessageDTO> getChatMessagesBySessionIdRecently(String sessionId, int limit);

    /**
     * 创建一条用户侧发起的聊天消息。
     * <p>
     * 该方法除了持久化消息外，还会发布聊天事件，驱动 Agent 异步执行。
     *
     * @param request 创建消息请求
     * @return 新建消息的主键
     */
    CreateChatMessageResponse createChatMessage(CreateChatMessageRequest request);

    /**
     * 直接根据 DTO 创建一条聊天消息。
     * <p>
     * 该入口通常给内部流程使用，不触发新的聊天事件。
     *
     * @param chatMessageDTO 运行时构造出的消息对象
     * @return 新建消息的主键
     */
    CreateChatMessageResponse createChatMessage(ChatMessageDTO chatMessageDTO);

    /**
     * 由 Agent 自身创建聊天消息。
     * <p>
     * 与普通创建的区别在于不会再次触发事件，避免 Agent 输出引发递归执行。
     *
     * @param request Agent 输出消息对应的请求对象
     * @return 新建消息的主键
     */
    CreateChatMessageResponse agentCreateChatMessage(CreateChatMessageRequest request);

    /**
     * 在已有消息内容后追加文本。
     * <p>
     * 适合流式或分段生成场景，将增量内容拼接到原消息中。
     *
     * @param chatMessageId 目标消息 ID
     * @param appendContent 需要追加的文本内容
     * @return 被追加消息的主键
     */
    CreateChatMessageResponse appendChatMessage(String chatMessageId, String appendContent);

    /**
     * 删除指定聊天消息。
     *
     * @param chatMessageId 消息 ID
     */
    void deleteChatMessage(String chatMessageId);

    /**
     * 更新指定聊天消息的内容或元数据。
     *
     * @param chatMessageId 消息 ID
     * @param request 更新请求
     */
    void updateChatMessage(String chatMessageId, UpdateChatMessageRequest request);
}
