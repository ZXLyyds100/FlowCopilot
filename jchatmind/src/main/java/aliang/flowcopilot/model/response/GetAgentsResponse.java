package aliang.flowcopilot.model.response;

import aliang.flowcopilot.model.vo.AgentVO;
import lombok.Builder;
import lombok.Data;

/**
 * Agent 列表响应对象。
 */
@Data
@Builder
public class GetAgentsResponse {
    private AgentVO[] agents;
}
