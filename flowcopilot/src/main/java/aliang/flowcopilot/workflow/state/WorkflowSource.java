package aliang.flowcopilot.workflow.state;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Knowledge source used by Retriever and displayed in workflow details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSource {
    private int index;
    private String sourceType;
    private String title;
    private String content;
    private String metadata;
}
