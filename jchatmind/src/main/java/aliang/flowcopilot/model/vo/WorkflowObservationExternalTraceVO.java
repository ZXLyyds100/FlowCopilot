package aliang.flowcopilot.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * External trace export state and trace URL information.
 */
@Data
@Builder
public class WorkflowObservationExternalTraceVO {
    private boolean enabled;
    private String provider;
    private String status;
    private String traceId;
    private String projectName;
    private String url;
    private String lastErrorMessage;
}
