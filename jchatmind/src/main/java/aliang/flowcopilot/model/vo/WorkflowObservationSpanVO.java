package aliang.flowcopilot.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Hierarchical observation span shown in workflow observability view.
 */
@Data
@Builder
public class WorkflowObservationSpanVO {
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
    private String modelName;
    private String responseId;
    private String finishReason;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private BigDecimal estimatedCostUsd;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Long durationMs;
    private WorkflowObservationSpanVO[] children;
}
