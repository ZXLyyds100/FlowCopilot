package aliang.flowcopilot.workflow.observability;

/**
 * Supported platform observation span types.
 */
public enum ObservationSpanType {
    WORKFLOW_RUN,
    NODE_RUN,
    LLM_CALL,
    TOOL_CALL,
    RETRIEVAL_CALL
}
