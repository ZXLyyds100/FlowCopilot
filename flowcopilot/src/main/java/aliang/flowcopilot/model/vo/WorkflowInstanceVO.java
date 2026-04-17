package aliang.flowcopilot.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Workflow instance view object for frontend display.
 */
@Data
@Builder
public class WorkflowInstanceVO {
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
