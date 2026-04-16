package aliang.flowcopilot.model.response;

import aliang.flowcopilot.model.vo.WorkflowInstanceVO;
import lombok.Builder;
import lombok.Data;

/**
 * Recent workflow list response.
 */
@Data
@Builder
public class GetWorkflowsResponse {
    private WorkflowInstanceVO[] workflows;
}
