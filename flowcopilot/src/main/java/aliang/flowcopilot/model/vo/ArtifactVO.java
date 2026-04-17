package aliang.flowcopilot.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Workflow deliverable view object.
 */
@Data
@Builder
public class ArtifactVO {
    private String id;
    private String workflowInstanceId;
    private String type;
    private String title;
    private String content;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
