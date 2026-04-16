package aliang.flowcopilot.model.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * A concrete workflow run created from a user task.
 */
@Data
@Builder
public class WorkflowInstance {
    private String id;
    private String definitionId;
    private String title;
    private String input;
    private String status;
    private String currentStep;
    private String result;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
