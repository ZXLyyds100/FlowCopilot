package aliang.flowcopilot.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Workflow checkpoint snapshot for the UI.
 */
@Data
@Builder
public class WorkflowExecutionCheckpointVO {
    private String id;
    private String workflowInstanceId;
    private String traceId;
    private String runId;
    private String nodeKey;
    private String checkpointType;
    private String stateSnapshot;
    private String metadata;
    private LocalDateTime createdAt;
}
