package aliang.flowcopilot.workflow.graph;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Serializable sub-graph definition executed inside a graph node.
 */
@Data
@Builder
public class WorkflowSubGraphDefinition {
    private String code;
    private String name;
    private String description;
    private List<WorkflowSubGraphGroup> groups;
}
