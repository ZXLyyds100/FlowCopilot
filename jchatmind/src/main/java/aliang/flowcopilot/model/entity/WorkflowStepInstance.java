package aliang.flowcopilot.model.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Execution record for a single workflow node.
 */
@Data
@Builder
public class WorkflowStepInstance {
    private String id;
    private String workflowInstanceId;
    private String nodeKey;
    private String nodeName;
    private String status;
    private String inputSnapshot;
    private String outputSnapshot;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
