package aliang.flowcopilot.model.response;

import lombok.Builder;
import lombok.Data;

/**
 * 创建 Agent 响应对象。
 */
@Data
@Builder
public class CreateAgentResponse {
    private String agentId;
}
