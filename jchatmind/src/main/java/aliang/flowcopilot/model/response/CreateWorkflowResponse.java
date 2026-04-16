package aliang.flowcopilot.model.response;

import aliang.flowcopilot.model.vo.WorkflowInstanceVO;
import lombok.Builder;
import lombok.Data;

/**
 * Response returned after starting a workflow.
 */
@Data
@Builder
public class CreateWorkflowResponse {
    private String workflowInstanceId;
    private WorkflowInstanceVO workflow;
}
