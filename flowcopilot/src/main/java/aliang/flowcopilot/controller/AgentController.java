package aliang.flowcopilot.controller;

import aliang.flowcopilot.model.common.ApiResponse;
import aliang.flowcopilot.model.request.CreateAgentRequest;
import aliang.flowcopilot.model.request.UpdateAgentRequest;
import aliang.flowcopilot.model.response.CreateAgentResponse;
import aliang.flowcopilot.model.response.GetAgentsResponse;
import aliang.flowcopilot.service.AgentFacadeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Agent 管理控制器。
 * <p>
 * 对外暴露 Agent 的查询、创建、删除和更新接口，
 * 供前端管理不同的智能体配置。
 */
@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class AgentController {

    private final AgentFacadeService agentFacadeService;

    /**
     * 查询全部 Agent。
     *
     * @return Agent 列表
     */
    @GetMapping("/agents")
    public ApiResponse<GetAgentsResponse> getAgents() {
        return ApiResponse.success(agentFacadeService.getAgents());
    }

    /**
     * 创建新的 Agent。
     *
     * @param request 创建请求
     * @return 新建 Agent 的 ID
     */
    @PostMapping("/agents")
    public ApiResponse<CreateAgentResponse> createAgent(@RequestBody CreateAgentRequest request) {
        return ApiResponse.success(agentFacadeService.createAgent(request));
    }

    /**
     * 删除指定 Agent。
     *
     * @param agentId Agent ID
     * @return 空成功响应
     */
    @DeleteMapping("/agents/{agentId}")
    public ApiResponse<Void> deleteAgent(@PathVariable String agentId) {
        agentFacadeService.deleteAgent(agentId);
        return ApiResponse.success();
    }

    /**
     * 更新指定 Agent 的配置。
     *
     * @param agentId Agent ID
     * @param request 更新请求
     * @return 空成功响应
     */
    @PatchMapping("/agents/{agentId}")
    public ApiResponse<Void> updateAgent(@PathVariable String agentId, @RequestBody UpdateAgentRequest request) {
        agentFacadeService.updateAgent(agentId, request);
        return ApiResponse.success();
    }
}
