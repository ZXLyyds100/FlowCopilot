package aliang.flowcopilot.workflow.state;

/**
 * Single workflow step execution status.
 */
public enum WorkflowStepStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    WAITING_APPROVAL,
    FAILED
}
