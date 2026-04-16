package aliang.flowcopilot.model.response;

import aliang.flowcopilot.model.vo.WorkflowObservationExternalTraceVO;
import aliang.flowcopilot.model.vo.WorkflowObservationSpanVO;
import aliang.flowcopilot.model.vo.WorkflowObservationSummaryVO;
import lombok.Builder;
import lombok.Data;

/**
 * Workflow observability tree response.
 */
@Data
@Builder
public class GetWorkflowObservabilityResponse {
    private String workflowInstanceId;
    private WorkflowObservationSummaryVO summary;
    private WorkflowObservationSpanVO[] spans;
    private WorkflowObservationExternalTraceVO externalTrace;
}
