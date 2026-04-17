package aliang.flowcopilot.workflow.ai;

import lombok.Builder;
import lombok.Data;

/**
 * Captures workflow LLM completion text together with observability metadata.
 */
@Data
@Builder
public class ChatCompletionResult {
    private String text;
    private String modelName;
    private String responseId;
    private String finishReason;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
}
