package aliang.flowcopilot.model.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Human approval record created by the Approval workflow node.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRecord {
    private String id;
    private String workflowInstanceId;
    private String status;
    private String title;
    private String summary;
    private String comment;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
