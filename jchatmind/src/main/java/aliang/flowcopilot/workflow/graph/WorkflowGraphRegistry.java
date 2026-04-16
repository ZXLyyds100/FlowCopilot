package aliang.flowcopilot.workflow.graph;

import aliang.flowcopilot.workflow.state.WorkflowState;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Registry of fourth-stage graph templates.
 */
@Service
public class WorkflowGraphRegistry {

    private final Map<String, WorkflowGraphDefinition> definitions;

    public WorkflowGraphRegistry() {
        List<WorkflowGraphNode> commonNodes = List.of(
                node("planner", "Planner Agent", "planner", false),
                node("retriever", "Retriever Agent", "retriever", false),
                node("executor", "Executor Agent", "executor", false),
                node("reviewer", "Reviewer Agent", "reviewer", false),
                node("approval", "Human Approval", "human", false),
                node("publish", "Reporter Agent", "reporter", false)
        );
        WorkflowGraphDefinition research = WorkflowGraphDefinition.builder()
                .code(WorkflowTemplateType.RESEARCH.code())
                .name(WorkflowTemplateType.RESEARCH.displayName())
                .description(WorkflowTemplateType.RESEARCH.description())
                .nodes(commonNodes)
                .edges(List.of(
                        edge(WorkflowGraphDefinition.START, "planner", "start"),
                        edge("planner", "retriever", "need knowledge"),
                        edge("retriever", "executor", "context ready"),
                        edge("executor", "reviewer", "draft"),
                        conditional("reviewer", "executor", "review retry", this::shouldRetryAfterReview),
                        conditional("reviewer", "approval", "review pass", state -> !shouldRetryAfterReview(state)),
                        edge("approval", "publish", "approved"),
                        edge("publish", WorkflowGraphDefinition.END, "done")
                ))
                .build();
        WorkflowGraphDefinition analysis = WorkflowGraphDefinition.builder()
                .code(WorkflowTemplateType.ANALYSIS.code())
                .name(WorkflowTemplateType.ANALYSIS.displayName())
                .description(WorkflowTemplateType.ANALYSIS.description())
                .nodes(commonNodes)
                .edges(List.of(
                        edge(WorkflowGraphDefinition.START, "planner", "start"),
                        edge("planner", "retriever", "collect evidence"),
                        edge("retriever", "executor", "analyze"),
                        edge("executor", "reviewer", "review"),
                        conditional("reviewer", "executor", "revise", this::shouldRetryAfterReview),
                        conditional("reviewer", "publish", "auto publish", state -> !shouldRetryAfterReview(state)),
                        edge("publish", WorkflowGraphDefinition.END, "done")
                ))
                .build();
        WorkflowGraphDefinition taskExecution = WorkflowGraphDefinition.builder()
                .code(WorkflowTemplateType.TASK_EXECUTION.code())
                .name(WorkflowTemplateType.TASK_EXECUTION.displayName())
                .description(WorkflowTemplateType.TASK_EXECUTION.description())
                .nodes(commonNodes.stream().map(node -> "retriever".equals(node.getKey())
                        ? node.toBuilder().subGraph(true).build()
                        : node).toList())
                .edges(List.of(
                        edge(WorkflowGraphDefinition.START, "planner", "start"),
                        conditional("planner", "retriever", "has kb", state -> state.getKnowledgeBaseId() != null && !state.getKnowledgeBaseId().isBlank()),
                        conditional("planner", "executor", "no kb", state -> state.getKnowledgeBaseId() == null || state.getKnowledgeBaseId().isBlank()),
                        edge("retriever", "executor", "context"),
                        edge("executor", "reviewer", "review"),
                        conditional("reviewer", "executor", "retry", this::shouldRetryAfterReview),
                        conditional("reviewer", "approval", "approval", state -> !shouldRetryAfterReview(state)),
                        edge("approval", "publish", "approved"),
                        edge("publish", WorkflowGraphDefinition.END, "done")
                ))
                .build();
        definitions = Map.of(
                research.getCode(), research,
                analysis.getCode(), analysis,
                taskExecution.getCode(), taskExecution
        );
    }

    public WorkflowGraphDefinition get(String templateCode) {
        return definitions.getOrDefault(WorkflowTemplateType.fromCode(templateCode).code(), definitions.get(WorkflowTemplateType.RESEARCH.code()));
    }

    public List<WorkflowGraphDefinition> all() {
        return List.copyOf(definitions.values());
    }

    private boolean shouldRetryAfterReview(WorkflowState state) {
        return state.getReview() != null && !state.getReview().isPassed() && state.getRetryCount() < 1;
    }

    private WorkflowGraphNode node(String key, String name, String role, boolean subGraph) {
        return WorkflowGraphNode.builder()
                .key(key)
                .name(name)
                .role(role)
                .subGraph(subGraph)
                .build();
    }

    private WorkflowGraphEdge edge(String source, String target, String label) {
        return WorkflowGraphEdge.builder()
                .source(source)
                .target(target)
                .label(label)
                .conditional(false)
                .build();
    }

    private WorkflowGraphEdge conditional(String source, String target, String label, java.util.function.Predicate<WorkflowState> condition) {
        return WorkflowGraphEdge.builder()
                .source(source)
                .target(target)
                .label(label)
                .conditional(true)
                .condition(condition)
                .build();
    }
}
