package aliang.flowcopilot.workflow.agent;

import lombok.Builder;
import lombok.Data;

/**
 * Role profile used to keep each workflow agent's responsibility explicit.
 */
@Data
@Builder
public class WorkflowAgentProfile {
    private WorkflowAgentRole role;
    private String displayName;
    private String responsibility;
    private String systemPrompt;
}
