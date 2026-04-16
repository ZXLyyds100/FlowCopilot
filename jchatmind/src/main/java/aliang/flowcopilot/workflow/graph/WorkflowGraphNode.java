package aliang.flowcopilot.workflow.graph;

import lombok.Builder;
import lombok.Data;

/**
 * Node metadata in a workflow graph template.
 */
@Data
@Builder(toBuilder = true)
public class WorkflowGraphNode {
    private String key;
    private String name;
    private String role;
    private boolean subGraph;
}
