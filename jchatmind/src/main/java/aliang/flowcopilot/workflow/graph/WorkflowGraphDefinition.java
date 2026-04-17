package aliang.flowcopilot.workflow.graph;

import aliang.flowcopilot.exception.BizException;
import aliang.flowcopilot.workflow.state.WorkflowState;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LangGraph-style workflow definition with nodes, normal edges and conditional edges.
 */
@Data
@Builder
public class WorkflowGraphDefinition {
    public static final String START = "__start__";
    public static final String END = "__end__";

    private String code;
    private String name;
    private String description;
    private List<WorkflowGraphNode> nodes;
    private List<WorkflowGraphEdge> edges;
    private List<WorkflowSubGraphDefinition> subGraphs;

    public String firstNode(WorkflowState state) {
        return nextNode(START, state);
    }

    public String nextNode(String source, WorkflowState state) {
        return edges.stream()
                .filter(edge -> edge.getSource().equals(source))
                .filter(edge -> edge.matches(state))
                .findFirst()
                .map(WorkflowGraphEdge::getTarget)
                .orElseThrow(() -> new BizException("Graph route not found from node: " + source));
    }

    public String mermaid() {
        String body = edges.stream()
                .map(edge -> {
                    String label = edge.getLabel() == null || edge.getLabel().isBlank() ? "" : "|%s|".formatted(edge.getLabel());
                    return "    %s -->%s %s".formatted(normalize(edge.getSource()), label, normalize(edge.getTarget()));
                })
                .collect(Collectors.joining("\n"));
        return "graph TD\n" + body;
    }

    public Map<String, WorkflowGraphNode> nodeMap() {
        return nodes.stream().collect(Collectors.toMap(WorkflowGraphNode::getKey, node -> node));
    }

    public WorkflowSubGraphDefinition subGraph(String subGraphCode) {
        if (subGraphs == null || subGraphCode == null || subGraphCode.isBlank()) {
            return null;
        }
        return subGraphs.stream()
                .filter(subGraph -> subGraphCode.equals(subGraph.getCode()))
                .findFirst()
                .orElse(null);
    }

    private String normalize(String node) {
        if (START.equals(node)) {
            return "START";
        }
        if (END.equals(node)) {
            return "END";
        }
        return node;
    }
}
