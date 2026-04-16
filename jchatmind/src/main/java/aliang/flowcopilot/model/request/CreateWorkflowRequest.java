package aliang.flowcopilot.model.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * Request used to start a first-stage FlowCopilot workflow.
 */
@Data
public class CreateWorkflowRequest {
    private String title;

    @JsonAlias("userRequirement")
    private String input;

    private String knowledgeBaseId;
    private String templateCode;
}
