package aliang.flowcopilot.workflow.state;

/**
 * Workflow runtime status.
 */
public enum WorkflowStatus {
    CREATED,
    RUNNING,
    WAITING_APPROVAL,
    COMPLETED,
    FAILED,
    REJECTED
}
