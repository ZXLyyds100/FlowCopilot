package aliang.flowcopilot.model.response;

import aliang.flowcopilot.model.vo.ArtifactVO;
import aliang.flowcopilot.model.vo.WorkflowInstanceVO;
import aliang.flowcopilot.model.vo.WorkflowStepInstanceVO;
import lombok.Builder;
import lombok.Data;

/**
 * Full workflow detail response.
 */
@Data
@Builder
public class GetWorkflowResponse {
    private WorkflowInstanceVO workflow;
    private WorkflowStepInstanceVO[] steps;
    private ArtifactVO[] artifacts;
}
