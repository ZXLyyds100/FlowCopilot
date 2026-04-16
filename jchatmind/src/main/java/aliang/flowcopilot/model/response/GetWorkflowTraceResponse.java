package aliang.flowcopilot.model.response;

import aliang.flowcopilot.model.vo.ExecutionTraceRefVO;
import lombok.Builder;
import lombok.Data;

/**
 * Workflow trace list response.
 */
@Data
@Builder
public class GetWorkflowTraceResponse {
    private String workflowInstanceId;
    private ExecutionTraceRefVO[] traces;
}
