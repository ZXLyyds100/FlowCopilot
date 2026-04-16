package aliang.flowcopilot.workflow.graph;

import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.Command;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.GraphDefinition.END;
import static org.bsc.langgraph4j.GraphDefinition.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Compiles FlowCopilot templates into LangGraph4j StateGraph topologies.
 * <p>
 * Runtime persistence and SSE still live in WorkflowRuntimeService, while this compiler
 * gives each template a real LangGraph4j graph shape for validation and visualization.
 */
@Service
public class LangGraph4jGraphCompiler {

    public String compileMermaid(WorkflowGraphDefinition definition) {
        try {
            StateGraph<LangGraph4jWorkflowState> graph = new StateGraph<>(LangGraph4jWorkflowState::new);
            for (WorkflowGraphNode node : definition.getNodes()) {
                graph.addNode(node.getKey(), node_async(state -> Map.of("lastNode", node.getKey())));
            }

            Map<String, List<WorkflowGraphEdge>> bySource = definition.getEdges().stream()
                    .collect(java.util.stream.Collectors.groupingBy(WorkflowGraphEdge::getSource));

            for (Map.Entry<String, List<WorkflowGraphEdge>> entry : bySource.entrySet()) {
                List<WorkflowGraphEdge> edges = entry.getValue();
                boolean hasConditional = edges.stream().anyMatch(WorkflowGraphEdge::isConditional);
                String source = normalize(entry.getKey());
                if (hasConditional) {
                    Map<String, String> mappings = new HashMap<>();
                    for (WorkflowGraphEdge edge : edges) {
                        mappings.put(edge.getLabel(), normalize(edge.getTarget()));
                    }
                    graph.addConditionalEdges(source, (state, config) ->
                            CompletableFuture.completedFuture(new Command(state.route(entry.getKey()).orElse(edges.get(0).getLabel()))), mappings);
                } else {
                    for (WorkflowGraphEdge edge : edges) {
                        graph.addEdge(source, normalize(edge.getTarget()));
                    }
                }
            }

            var compiled = graph.compile();
            return compiled.getGraph(GraphRepresentation.Type.MERMAID, definition.getName(), true).getContent();
        } catch (GraphStateException e) {
            return definition.mermaid();
        }
    }

    private String normalize(String node) {
        if (WorkflowGraphDefinition.START.equals(node)) {
            return START;
        }
        if (WorkflowGraphDefinition.END.equals(node)) {
            return END;
        }
        return node;
    }
}
