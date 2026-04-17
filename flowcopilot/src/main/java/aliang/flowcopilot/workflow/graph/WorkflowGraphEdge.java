package aliang.flowcopilot.workflow.graph;

import aliang.flowcopilot.workflow.state.WorkflowState;
import lombok.Builder;
import lombok.Data;

/**
 * Directed graph edge. Conditional edges evaluate a predicate against WorkflowState.
 */
@Data
@Builder
public class WorkflowGraphEdge {
    private String source;
    private String target;
    private String label;
    private boolean conditional;
    private String conditionKey;

    public boolean matches(WorkflowState state) {
        if (!conditional || conditionKey == null || conditionKey.isBlank()) {
            return true;
        }
        return switch (conditionKey) {
            case "review_retry" -> state.getReview() != null && !state.getReview().isPassed() && state.getRetryCount() < 1;
            case "review_pass" -> state.getReview() == null || state.getReview().isPassed() || state.getRetryCount() >= 1;
            case "has_kb" -> state.getKnowledgeBaseId() != null && !state.getKnowledgeBaseId().isBlank();
            case "no_kb" -> state.getKnowledgeBaseId() == null || state.getKnowledgeBaseId().isBlank();
            default -> true;
        };
    }
}
