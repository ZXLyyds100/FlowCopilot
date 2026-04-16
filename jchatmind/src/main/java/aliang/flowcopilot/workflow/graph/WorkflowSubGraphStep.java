package aliang.flowcopilot.workflow.graph;

import lombok.Builder;
import lombok.Data;

/**
 * Single step inside a sub-graph group.
 */
@Data
@Builder
public class WorkflowSubGraphStep {
    private String key;
    private String name;
    private String handlerType;
}
