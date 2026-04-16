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
    private String plan;
    private List<String> retrievedContents;
    private String draftResult;
    private String finalOutput;
    private String artifactId;

    public List<String> safeRetrievedContents() {
        if (retrievedContents == null) {
            retrievedContents = new ArrayList<>();
        }
        return retrievedContents;
    }
}
