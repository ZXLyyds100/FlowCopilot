package aliang.flowcopilot.workflow.node;

import aliang.flowcopilot.workflow.agent.AgentRoleService;
import aliang.flowcopilot.workflow.agent.WorkflowAgentProfile;
import aliang.flowcopilot.workflow.agent.WorkflowAgentRole;
import aliang.flowcopilot.workflow.ai.StructuredOutputService;
import aliang.flowcopilot.workflow.state.WorkflowReview;
import aliang.flowcopilot.workflow.state.WorkflowState;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Reviews the draft before the final report is published.
 */
@Component
@AllArgsConstructor
public class ReviewerNode implements WorkflowNode {

    private static final String NO_REVISION_REQUEST = "No human revision request";

    private final AgentRoleService agentRoleService;
    private final StructuredOutputService structuredOutputService;

    @Override
    public String key() {
        return "reviewer";
    }

    @Override
    public String name() {
        return "Reviewer Agent";
    }

    @Override
    public WorkflowState execute(WorkflowState state) {
        int score = score(state);
        List<String> suggestions = suggestions(state, score);
        boolean passed = score >= 70;
        String revisionContext = formatRevisionContext(state);
        String fallback = """
                ## Quality Decision
                %s, score: %d/100.

                ## Main Checks
                - Covers the user task: %s
                - Includes the execution plan: %s
                - Uses retrieved knowledge: %s
                - Responds to the human revision request: %s

                ## Suggestions
                %s
                """.formatted(
                passed ? "Passed" : "Needs changes",
                score,
                hasText(state.getDraft()) ? "yes" : "no",
                hasText(state.getPlan()) ? "yes" : "no",
                state.safeSources().isEmpty() ? "no" : "yes",
                hasText(state.getRevisionRequest()) ? "yes, " + revisionContext : "n/a",
                String.join("\n", suggestions.stream().map(item -> "- " + item).toList())
        ).strip();

        WorkflowAgentProfile profile = agentRoleService.getProfile(WorkflowAgentRole.REVIEWER);
        String comment = structuredOutputService.generateOrFallback(profile, """
                User task: %s
                Execution plan: %s
                Knowledge sources: %s
                Draft: %s
                Human revision request: %s
                If there is a human revision request, explain whether the draft addresses it.
                Review the draft quality and provide a score, risks, and revision suggestions.
                """.formatted(state.getUserInput(), state.getPlan(), state.getRetrievedContents(), state.getDraft(), revisionContext), fallback);

        WorkflowReview review = WorkflowReview.builder()
                .score(score)
                .passed(passed)
                .comment(comment)
                .suggestions(suggestions)
                .build();
        state.setReview(review);
        state.setReviewComment(comment);
        return state;
    }

    private int score(WorkflowState state) {
        int score = 50;
        if (hasText(state.getDraft())) {
            score += 20;
        }
        if (hasText(state.getPlan())) {
            score += 10;
        }
        if (!state.safeSources().isEmpty()) {
            score += 10;
        }
        if (state.getDraft() != null && state.getDraft().length() > 300) {
            score += 10;
        }
        return Math.min(score, 100);
    }

    private List<String> suggestions(WorkflowState state, int score) {
        List<String> suggestions = new ArrayList<>();
        if (!hasText(state.getDraft())) {
            suggestions.add("Add draft content so the Executor Agent produces a concrete deliverable.");
        }
        if (state.safeSources().isEmpty()) {
            suggestions.add("Add knowledge sources so the final output is traceable.");
        }
        if (hasText(state.getRevisionRequest())) {
            suggestions.add("Check each human revision point and confirm the new draft addresses it.");
        }
        if (score < 90) {
            suggestions.add("Preserve the Reviewer Agent feedback in the final artifact for human confirmation.");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("The draft is complete enough to move to the Reporter Agent.");
        }
        return suggestions;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String formatRevisionContext(WorkflowState state) {
        if (!StringUtils.hasText(state.getRevisionRequest())) {
            return NO_REVISION_REQUEST;
        }
        return state.getRevisionRequest().trim();
    }
}
