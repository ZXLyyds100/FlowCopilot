package aliang.flowcopilot.model.request;

import aliang.flowcopilot.model.dto.AgentDTO;
import lombok.Data;

import java.util.List;

/**
 * 更新 Agent 请求对象。
 */
@Data
public class UpdateAgentRequest {
    private String name;
    private String description;
    private String systemPrompt;
    private String model;
    private List<String> allowedTools;
    private List<String> allowedKbs;
    private AgentDTO.ChatOptions chatOptions;
}
