package aliang.flowcopilot.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Persistent workflow checkpoint snapshot.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecutionCheckpoint {
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
