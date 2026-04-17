package aliang.flowcopilot.model.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Workflow template stored in the database.
 */
@Data
@Builder
public class WorkflowDefinition {
    private String id;
    private String code;
    private String name;
    private String description;
    private String definition;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
