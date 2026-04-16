package aliang.flowcopilot.workflow.node;

import aliang.flowcopilot.workflow.state.WorkflowState;

/**
 * Standard node contract for the first-stage fixed workflow skeleton.
 */
public interface WorkflowNode {
    String key();

    String name();

    WorkflowState execute(WorkflowState state);
}
