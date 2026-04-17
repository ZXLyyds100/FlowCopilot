package aliang.flowcopilot.model.response;

import aliang.flowcopilot.model.vo.WorkflowTemplateVO;
import lombok.Builder;
import lombok.Data;

/**
 * Workflow graph template list response.
 */
@Data
@Builder
public class GetWorkflowTemplatesResponse {
    private WorkflowTemplateVO[] templates;
}
