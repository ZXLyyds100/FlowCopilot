package aliang.flowcopilot.workflow.agent;

import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal second-stage tool permission registry.
 * <p>
 * The current workflow nodes are still deterministic Java nodes, but this
 * registry makes role-level tool boundaries explicit before ReAct is introduced.
 */
@Service
public class ToolRegistry {

    private final Map<WorkflowAgentRole, List<String>> allowedTools = new EnumMap<>(WorkflowAgentRole.class);

    public ToolRegistry() {
        allowedTools.put(WorkflowAgentRole.PLANNER, List.of("task_classifier", "plan_writer"));
        allowedTools.put(WorkflowAgentRole.RETRIEVER, List.of("knowledge_search"));
        allowedTools.put(WorkflowAgentRole.EXECUTOR, List.of("draft_writer"));
        allowedTools.put(WorkflowAgentRole.REVIEWER, List.of("quality_review"));
        allowedTools.put(WorkflowAgentRole.REPORTER, List.of("markdown_report"));
    }

    public List<String> allowedTools(WorkflowAgentRole role) {
        return allowedTools.getOrDefault(role, List.of());
    }
}
