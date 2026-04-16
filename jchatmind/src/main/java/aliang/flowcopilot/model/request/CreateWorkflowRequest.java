package aliang.flowcopilot.model.request;

import lombok.Data;

/**
 * Request used to start a first-stage FlowCopilot workflow.
 */
@Data
public class CreateWorkflowRequest {
    private String title;
    private String input;
    private String knowledgeBaseId;
}
