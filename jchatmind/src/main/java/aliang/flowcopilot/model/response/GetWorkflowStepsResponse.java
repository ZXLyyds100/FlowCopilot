package aliang.flowcopilot.model.response;

import aliang.flowcopilot.model.vo.WorkflowStepInstanceVO;
import lombok.Builder;
import lombok.Data;

/**
 * Workflow step list response.
 */
@Data
@Builder
public class GetWorkflowStepsResponse {
    private WorkflowStepInstanceVO[] steps;
}
