package aliang.flowcopilot.model.request;

import lombok.Data;

/**
 * Update request for a persisted workflow template.
 */
@Data
public class UpdateWorkflowTemplateRequest {
    private String name;
    private String description;
    private String definitionJson;
}
