package com.kama.jchatmind.service;

import com.kama.jchatmind.model.request.CreateAgentRequest;
import com.kama.jchatmind.model.request.UpdateAgentRequest;
import com.kama.jchatmind.model.response.CreateAgentResponse;
import com.kama.jchatmind.model.response.GetAgentsResponse;

/**
 * 智能体配置门面服务。
 * <p>
 * 该接口对外暴露 Agent 的核心管理能力，负责承接 Controller 的调用，
 * 并协调参数校验、对象转换和持久化操作。
 */
public interface AgentFacadeService {
    /**
     * 查询系统中的全部 Agent 配置。
     *
     * @return 包含全部 Agent 展示对象的响应结果
     */
    GetAgentsResponse getAgents();

    /**
     * 创建一个新的 Agent。
     *
     * @param request 创建 Agent 所需的名称、模型、工具、知识库等配置
     * @return 新建 Agent 的主键标识
     */
    CreateAgentResponse createAgent(CreateAgentRequest request);

    /**
     * 删除指定的 Agent。
     *
     * @param agentId 待删除 Agent 的主键
     */
    void deleteAgent(String agentId);

    /**
     * 更新指定 Agent 的配置。
     *
     * @param agentId 待更新 Agent 的主键
     * @param request 允许局部更新的 Agent 配置项
     */
    void updateAgent(String agentId, UpdateAgentRequest request);
}
