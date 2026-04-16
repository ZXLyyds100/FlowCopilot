package aliang.flowcopilot.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * Workflow graph template metadata.
 */
@Data
@Builder
public class WorkflowTemplateVO {
    private String code;
    private String name;
    private String description;
    private String mermaid;
}
