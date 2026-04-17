package aliang.flowcopilot.model.vo;

import aliang.flowcopilot.model.dto.AgentDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Agent 前端展示对象。
 */
@Data
@Builder
public class AgentVO {
    private String id;

    private String name;

    private String description;

    private String systemPrompt;

    private AgentDTO.ModelType model;

    private List<String> allowedTools;

    private List<String> allowedKbs;

    private AgentDTO.ChatOptions chatOptions;
}
