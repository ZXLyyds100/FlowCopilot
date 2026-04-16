package aliang.flowcopilot.workflow.state;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Reviewer output kept in WorkflowState.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowReview {
    private int score;
    private boolean passed;
    private String comment;
    private List<String> suggestions;
}
