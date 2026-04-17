package aliang.flowcopilot.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight trace reference for graph-level observability.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionTraceRef {
    private String id;
    private String workflowInstanceId;
    private String traceId;
    private String graphTemplate;
    private String nodeKey;
    private String eventType;
    private String status;
    private String inputSnapshot;
    private String outputSnapshot;
    private String errorMessage;
    private Long durationMs;
    private LocalDateTime createdAt;
}
