package aliang.flowcopilot.model.response;

import aliang.flowcopilot.model.vo.WorkflowExecutionCheckpointVO;
import lombok.Builder;
import lombok.Data;

/**
 * Workflow checkpoint list response.
 */
@Data
@Builder
public class GetWorkflowCheckpointsResponse {
    private String workflowInstanceId;
    private WorkflowExecutionCheckpointVO[] checkpoints;
}
