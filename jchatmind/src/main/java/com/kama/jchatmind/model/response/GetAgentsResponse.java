package com.kama.jchatmind.model.response;

import com.kama.jchatmind.model.vo.AgentVO;
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
