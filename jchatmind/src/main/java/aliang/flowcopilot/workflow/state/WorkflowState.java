package aliang.flowcopilot.workflow.state;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Explicit state object shared by all first-stage workflow nodes.
 */
@Data
@Builder
public class WorkflowState {
    private String workflowInstanceId;
    private String title;
    private String userInput;
    private String knowledgeBaseId;
    private String taskType;
    private String plan;
    private List<String> retrievedContents;
    private List<WorkflowSource> sources;
    private String draft;
    private String draftResult;
    private WorkflowReview review;
    private String reviewComment;
    private String finalOutput;
    private String artifactId;

    public List<String> safeRetrievedContents() {
        if (retrievedContents == null) {
            retrievedContents = new ArrayList<>();
        } else if (!(retrievedContents instanceof ArrayList)) {
            retrievedContents = new ArrayList<>(retrievedContents);
        }
        return retrievedContents;
    }

    public List<WorkflowSource> safeSources() {
        if (sources == null) {
            sources = new ArrayList<>();
        } else if (!(sources instanceof ArrayList)) {
            sources = new ArrayList<>(sources);
        }
        return sources;
    }
}
