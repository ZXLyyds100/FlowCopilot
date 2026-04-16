package aliang.flowcopilot.workflow.state;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Reviewer output kept in WorkflowState.
 */
@Data
@Builder
public class WorkflowReview {
    private int score;
    private boolean passed;
    private String comment;
    private List<String> suggestions;
}
