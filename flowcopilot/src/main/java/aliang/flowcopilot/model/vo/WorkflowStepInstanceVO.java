package aliang.flowcopilot.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Workflow step execution view object.
 */
@Data
@Builder
public class WorkflowStepInstanceVO {
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
