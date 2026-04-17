package aliang.flowcopilot.workflow.graph;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * A sub-graph group can run sequentially or in parallel.
 */
@Data
@Builder
public class WorkflowSubGraphGroup {
    private String key;
    private String name;
    private boolean parallel;
    private List<WorkflowSubGraphStep> steps;
}
