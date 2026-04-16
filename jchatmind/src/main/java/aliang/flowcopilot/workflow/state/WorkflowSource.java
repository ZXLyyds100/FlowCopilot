package aliang.flowcopilot.workflow.state;

import lombok.Builder;
import lombok.Data;

/**
 * Knowledge source used by Retriever and displayed in workflow details.
 */
@Data
@Builder
public class WorkflowSource {
    private int index;
    private String sourceType;
    private String title;
    private String content;
    private String metadata;
}
