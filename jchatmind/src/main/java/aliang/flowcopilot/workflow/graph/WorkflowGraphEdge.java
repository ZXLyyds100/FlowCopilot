package aliang.flowcopilot.workflow.graph;

import aliang.flowcopilot.workflow.state.WorkflowState;
import lombok.Builder;
import lombok.Data;

import java.util.function.Predicate;

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
    private Predicate<WorkflowState> condition;

    public boolean matches(WorkflowState state) {
        return condition == null || condition.test(state);
    }
}
