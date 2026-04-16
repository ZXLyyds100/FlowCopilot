package aliang.flowcopilot.service;

import aliang.flowcopilot.model.request.CreateChatSessionRequest;
import aliang.flowcopilot.model.request.UpdateChatSessionRequest;
import aliang.flowcopilot.model.response.CreateChatSessionResponse;
import aliang.flowcopilot.model.response.GetChatSessionResponse;
import aliang.flowcopilot.model.response.GetChatSessionsResponse;

/**
 * 聊天会话门面服务。
 * <p>
 * 负责会话的创建、查询、更新、删除，以及按 Agent 维度组织会话列表。
 */
public interface ChatSessionFacadeService {
    /**
     * 查询系统中的全部聊天会话。
     *
     * @return 会话列表
     */
    GetChatSessionsResponse getChatSessions();

    /**
     * 查询单个聊天会话详情。
     *
     * @param chatSessionId 会话 ID
     * @return 单会话详情
     */
    GetChatSessionResponse getChatSession(String chatSessionId);

    /**
     * 按 Agent 查询其关联的会话集合。
     *
     * @param agentId Agent ID
     * @return 该 Agent 关联的全部会话
     */
    GetChatSessionsResponse getChatSessionsByAgentId(String agentId);

    /**
     * 创建新的聊天会话。
     *
     * @param request 会话创建请求
     * @return 新建会话的主键
     */
    CreateChatSessionResponse createChatSession(CreateChatSessionRequest request);

    /**
     * 删除指定会话。
     *
     * @param chatSessionId 会话 ID
     */
    void deleteChatSession(String chatSessionId);

    /**
     * 更新会话标题等可编辑信息。
     *
     * @param chatSessionId 会话 ID
     * @param request 更新请求
     */
    void updateChatSession(String chatSessionId, UpdateChatSessionRequest request);
}
