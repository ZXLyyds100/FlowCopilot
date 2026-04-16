package aliang.flowcopilot.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Approval record view for the third-stage workflow UI.
 */
@Data
@Builder
public class ApprovalRecordVO {
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
