package aliang.flowcopilot.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import aliang.flowcopilot.converter.AgentConverter;
import aliang.flowcopilot.exception.BizException;
import aliang.flowcopilot.mapper.AgentMapper;
import aliang.flowcopilot.model.dto.AgentDTO;
import aliang.flowcopilot.model.entity.Agent;
import aliang.flowcopilot.model.request.CreateAgentRequest;
import aliang.flowcopilot.model.request.UpdateAgentRequest;
import aliang.flowcopilot.model.response.CreateAgentResponse;
import aliang.flowcopilot.model.response.GetAgentsResponse;
import aliang.flowcopilot.model.vo.AgentVO;
import aliang.flowcopilot.service.AgentFacadeService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent 门面服务实现。
 * <p>
 * 负责完成 Agent 配置相关的业务编排，包括对象转换、字段校验和数据库持久化。
 */
@Service
@AllArgsConstructor
public class AgentFacadeServiceImpl implements AgentFacadeService {

    private final AgentMapper agentMapper;
    private final AgentConverter agentConverter;

    @Override
    /**
     * 查询全部 Agent，并转换为前端可直接展示的 VO。
     *
     * @return Agent 列表响应
     */
    public GetAgentsResponse getAgents() {
        List<Agent> agents = agentMapper.selectAll();
        List<AgentVO> result = new ArrayList<>();
        for (Agent agent : agents) {
            try {
                AgentVO vo = agentConverter.toVO(agent);
                result.add(vo);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return GetAgentsResponse.builder()
                .agents(result.toArray(new AgentVO[0]))
                .build();
    }

    @Override
    /**
     * 创建新的 Agent 配置记录。
     *
     * @param request 创建请求
     * @return 新建 Agent 的主键
     */
    public CreateAgentResponse createAgent(CreateAgentRequest request) {
        try {
            // 将 CreateAgentRequest 转换为 AgentDTO
            AgentDTO agentDTO = agentConverter.toDTO(request);
            
            // 将 AgentDTO 转换为 Agent 实体
            Agent agent = agentConverter.toEntity(agentDTO);
            
            // 设置创建时间和更新时间
            LocalDateTime now = LocalDateTime.now();
            agent.setCreatedAt(now);
            agent.setUpdatedAt(now);
            
            // 插入数据库，ID 由数据库自动生成
            int result = agentMapper.insert(agent);
            if (result <= 0) {
                throw new BizException("创建 agent 失败");
            }
            
            // 返回生成的 agentId
            return CreateAgentResponse.builder()
                    .agentId(agent.getId())
                    .build();
        } catch (JsonProcessingException e) {
            throw new BizException("创建 agent 时发生序列化错误: " + e.getMessage());
        }
    }

    @Override
    /**
     * 删除指定 Agent。
     *
     * @param agentId Agent ID
     */
    public void deleteAgent(String agentId) {
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new BizException("Agent 不存在: " + agentId);
        }
        
        int result = agentMapper.deleteById(agentId);
        if (result <= 0) {
            throw new BizException("删除 agent 失败");
        }
    }

    @Override
    /**
     * 更新指定 Agent 的配置。
     *
     * @param agentId Agent ID
     * @param request 更新请求
     */
    public void updateAgent(String agentId, UpdateAgentRequest request) {
        try {
            // 查询现有的 agent
            Agent existingAgent = agentMapper.selectById(agentId);
            if (existingAgent == null) {
                throw new BizException("Agent 不存在: " + agentId);
            }
            
            // 将现有 Agent 转换为 AgentDTO
            AgentDTO agentDTO = agentConverter.toDTO(existingAgent);
            
            // 使用 UpdateAgentRequest 更新 AgentDTO
            agentConverter.updateDTOFromRequest(agentDTO, request);
            
            // 将更新后的 AgentDTO 转换回 Agent 实体
            Agent updatedAgent = agentConverter.toEntity(agentDTO);
            
            // 保留原有的 ID 和创建时间
            updatedAgent.setId(existingAgent.getId());
            updatedAgent.setCreatedAt(existingAgent.getCreatedAt());
            updatedAgent.setUpdatedAt(LocalDateTime.now());
            
            // 更新数据库
            int result = agentMapper.updateById(updatedAgent);
            if (result <= 0) {
                throw new BizException("更新 agent 失败");
            }
        } catch (JsonProcessingException e) {
            throw new BizException("更新 agent 时发生序列化错误: " + e.getMessage());
        }
    }
}
