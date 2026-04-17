package aliang.flowcopilot.workflow.node;

import aliang.flowcopilot.workflow.agent.AgentRoleService;
import aliang.flowcopilot.workflow.agent.WorkflowAgentProfile;
import aliang.flowcopilot.workflow.agent.WorkflowAgentRole;
import aliang.flowcopilot.workflow.ai.StructuredOutputService;
import aliang.flowcopilot.workflow.state.WorkflowSource;
import aliang.flowcopilot.workflow.state.WorkflowState;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

/**
 * Produces the main execution draft from plan and retrieved context.
 */
@Component
@AllArgsConstructor
public class ExecutorNode implements WorkflowNode {

    private static final String NO_REVISION_REQUEST = "No human revision request";

    private final AgentRoleService agentRoleService;
    private final StructuredOutputService structuredOutputService;

    @Override
    public String key() {
        return "executor";
    }

    @Override
    public String name() {
        return "Executor Agent";
    }

    @Override
    public WorkflowState execute(WorkflowState state) {
        String context = state.safeSources()
                .stream()
                .map(this::formatSource)
                .collect(Collectors.joining("\n"));
        String revisionContext = formatRevisionContext(state);
        String fallback = """
                ## Task Understanding
                %s

                ## Task Type
                %s

                ## Execution Plan
                %s

                ## Knowledge Sources
                %s

                ## Human Revision Request
                %s

                ## Draft
                This draft is generated from the Planner Agent output and the Retriever Agent context.
                """.formatted(state.getUserInput(), state.getTaskType(), state.getPlan(), context, revisionContext);
        WorkflowAgentProfile profile = agentRoleService.getProfile(WorkflowAgentRole.EXECUTOR);
        String draft = structuredOutputService.generateOrFallback(profile, """
                User task: %s
                Task type: %s
                Execution plan: %s
                Knowledge sources: %s
                Human revision request: %s
                If there is a human revision request, address it explicitly in the regenerated draft.
                Generate a complete draft for the Reviewer Agent.
                """.formatted(state.getUserInput(), state.getTaskType(), state.getPlan(), context, revisionContext), fallback);
        state.setDraft(draft.strip());
        state.setDraftResult(draft.strip());
        return state;
    }

    private String formatRevisionContext(WorkflowState state) {
        if (!StringUtils.hasText(state.getRevisionRequest())) {
            return NO_REVISION_REQUEST;
        }
        return state.getRevisionRequest().trim();
    }

    private String formatSource(WorkflowSource source) {
        return "- [%d] %s: %s".formatted(source.getIndex(), source.getTitle(), source.getContent());
    }
}
