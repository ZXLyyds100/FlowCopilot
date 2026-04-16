package aliang.flowcopilot.model.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Workflow observability summary counters.
 */
@Data
@Builder
public class WorkflowObservationSummaryVO {
    private int totalSpans;
    private int workflowRuns;
    private int nodeRuns;
    private int llmCalls;
    private int toolCalls;
    private int retrievalCalls;
    private int errorCount;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private BigDecimal estimatedCostUsd;
    private String exporterStatus;
    private String latestTraceId;
}
