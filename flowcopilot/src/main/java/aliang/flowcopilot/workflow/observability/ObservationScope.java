package aliang.flowcopilot.workflow.observability;

import aliang.flowcopilot.model.entity.ExecutionObservation;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * In-flight observation scope tracked in the current workflow thread.
 */
@Getter
@Builder
public class ObservationScope {
    private boolean noop;
    private String id;
    private String runId;
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String workflowInstanceId;
    private String nodeKey;
    private ObservationSpanType spanType;
    private String name;
    private String inputSummary;
    private LocalDateTime startedAt;
    private Map<String, Object> attributes;
    private ObservationExporter.ExportedSpanHandle exportedHandle;

    public ExecutionObservation toRunningObservation(String attributesJson) {
        return ExecutionObservation.builder()
                .id(id)
                .runId(runId)
                .traceId(traceId)
                .spanId(spanId)
                .parentSpanId(parentSpanId)
                .workflowInstanceId(workflowInstanceId)
                .nodeKey(nodeKey)
                .spanType(spanType.name())
                .name(name)
                .status(ObservationStatus.RUNNING.name())
                .inputSummary(inputSummary)
                .attributesJson(attributesJson)
                .startedAt(startedAt)
                .createdAt(startedAt)
                .updatedAt(startedAt)
                .build();
    }

    public static ObservationScope noop() {
        return ObservationScope.builder().noop(true).build();
    }
}
