package aliang.flowcopilot.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Normalized observability record for workflow/platform telemetry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionObservation {
    private String id;
    private String runId;
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String workflowInstanceId;
    private String nodeKey;
    private String spanType;
    private String name;
    private String status;
    private String inputSummary;
    private String outputSummary;
    private String errorMessage;
    private String attributesJson;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Long durationMs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
