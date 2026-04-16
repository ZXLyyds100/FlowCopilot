package aliang.flowcopilot.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Trace reference shown in workflow observability views.
 */
@Data
@Builder
public class ExecutionTraceRefVO {
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
