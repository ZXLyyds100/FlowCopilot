package aliang.flowcopilot.workflow.observability;

import aliang.flowcopilot.config.FlowCopilotObservabilityProperties;
import aliang.flowcopilot.model.entity.ExecutionObservation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Records workflow observation scopes to the local store and optional exporter.
 */
@Service
@AllArgsConstructor
public class ObservationRecorder {

    private static final ThreadLocal<Deque<ObservationScope>> ACTIVE_SCOPES = ThreadLocal.withInitial(ArrayDeque::new);

    private final ObservationStore observationStore;
    private final ObservationExporter observationExporter;
    private final FlowCopilotObservabilityProperties properties;
    private final ObjectMapper objectMapper;

    public String nextTraceId() {
        return hex(32);
    }

    public String nextRunId() {
        return hex(16);
    }

    public ObservationScope startWorkflowRun(String workflowInstanceId,
                                             String traceId,
                                             String runId,
                                             String name,
                                             String startNodeKey,
                                             String inputSummary,
                                             Map<String, Object> attributes) {
        return startScope(workflowInstanceId, traceId, runId, null, ObservationSpanType.WORKFLOW_RUN, name, startNodeKey, inputSummary, attributes);
    }

    public ObservationScope startNodeRun(String workflowInstanceId,
                                         String runId,
                                         String nodeKey,
                                         String name,
                                         String inputSummary,
                                         Map<String, Object> attributes) {
        ObservationScope parent = currentScope();
        String traceId = parent != null ? parent.getTraceId() : nextTraceId();
        return startScope(workflowInstanceId, traceId, runId, parent, ObservationSpanType.NODE_RUN, name, nodeKey, inputSummary, attributes);
    }

    public ObservationScope startNodeRun(String workflowInstanceId,
                                         String runId,
                                         ObservationScope parent,
                                         String nodeKey,
                                         String name,
                                         String inputSummary,
                                         Map<String, Object> attributes) {
        String traceId = parent != null ? parent.getTraceId() : nextTraceId();
        return startScope(workflowInstanceId, traceId, runId, parent, ObservationSpanType.NODE_RUN, name, nodeKey, inputSummary, attributes);
    }

    public ObservationScope startChildSpan(ObservationSpanType spanType,
                                           String name,
                                           String nodeKey,
                                           String inputSummary,
                                           Map<String, Object> attributes) {
        ObservationScope parent = currentScope();
        if (parent == null || !properties.isEnabled()) {
            return ObservationScope.noop();
        }
        return startScope(parent.getWorkflowInstanceId(), parent.getTraceId(), parent.getRunId(), parent, spanType, name, nodeKey, inputSummary, attributes);
    }

    public void complete(ObservationScope scope, ObservationStatus status, String outputSummary, Map<String, Object> extraAttributes) {
        if (scope == null || scope.isNoop()) {
            return;
        }
        Map<String, Object> mergedAttributes = merge(scope.getAttributes(), extraAttributes);
        LocalDateTime now = LocalDateTime.now();
        if (properties.isPersistEnabled()) {
            observationStore.update(ExecutionObservation.builder()
                    .id(scope.getId())
                    .status(status.name())
                    .outputSummary(summarize(outputSummary))
                    .attributesJson(toJson(mergedAttributes))
                    .endedAt(now)
                    .durationMs(ChronoUnit.MILLIS.between(scope.getStartedAt(), now))
                    .build());
        }
        try {
            observationExporter.complete(scope.getExportedHandle(), summarize(outputSummary), mergedAttributes);
        } catch (Exception ignored) {
            // exporter failures must not break workflow execution
        }
        popScope(scope.getId());
    }

    public void fail(ObservationScope scope, Throwable error, Map<String, Object> extraAttributes) {
        if (scope == null || scope.isNoop()) {
            return;
        }
        Map<String, Object> mergedAttributes = merge(scope.getAttributes(), extraAttributes);
        LocalDateTime now = LocalDateTime.now();
        if (properties.isPersistEnabled()) {
            observationStore.update(ExecutionObservation.builder()
                    .id(scope.getId())
                    .status(ObservationStatus.FAILED.name())
                    .errorMessage(error == null ? null : summarize(error.getMessage()))
                    .attributesJson(toJson(mergedAttributes))
                    .endedAt(now)
                    .durationMs(ChronoUnit.MILLIS.between(scope.getStartedAt(), now))
                    .build());
        }
        try {
            observationExporter.fail(scope.getExportedHandle(), error, mergedAttributes);
        } catch (Exception ignored) {
            // exporter failures must not break workflow execution
        }
        popScope(scope.getId());
    }

    public void clearCurrentThread() {
        ACTIVE_SCOPES.remove();
    }

    public ObservationScope currentScope() {
        return ACTIVE_SCOPES.get().peek();
    }

    public ObservationExporter exporter() {
        return observationExporter;
    }

    public List<ExecutionObservation> getObservations(String workflowInstanceId) {
        if (!properties.isPersistEnabled()) {
            return List.of();
        }
        return observationStore.findByWorkflowInstanceId(workflowInstanceId);
    }

    private ObservationScope startScope(String workflowInstanceId,
                                        String traceId,
                                        String runId,
                                        ObservationScope parent,
                                        ObservationSpanType spanType,
                                        String name,
                                        String nodeKey,
                                        String inputSummary,
                                        Map<String, Object> attributes) {
        if (!properties.isEnabled()) {
            return ObservationScope.noop();
        }
        String fallbackSpanId = hex(16);
        ObservationExporter.ExportedSpanHandle exportedHandle = observationExporter.start(
                new ObservationExporter.ExportObservationRequest(
                        name,
                        spanType,
                        traceId,
                        fallbackSpanId,
                        summarize(inputSummary),
                        attributes == null ? Map.of() : attributes
                ),
                parent == null ? null : parent.getExportedHandle()
        );
        LocalDateTime now = LocalDateTime.now();
        ObservationScope scope = ObservationScope.builder()
                .noop(false)
                .id(UUID.randomUUID().toString())
                .runId(runId)
                .traceId(exportedHandle.traceId())
                .spanId(exportedHandle.spanId())
                .parentSpanId(parent == null ? null : parent.getSpanId())
                .workflowInstanceId(workflowInstanceId)
                .nodeKey(nodeKey)
                .spanType(spanType)
                .name(name)
                .inputSummary(summarize(inputSummary))
                .startedAt(now)
                .attributes(attributes == null ? Map.of() : new LinkedHashMap<>(attributes))
                .exportedHandle(exportedHandle)
                .build();
        if (properties.isPersistEnabled()) {
            observationStore.create(scope.toRunningObservation(toJson(scope.getAttributes())));
        }
        ACTIVE_SCOPES.get().push(scope);
        return scope;
    }

    private Map<String, Object> merge(Map<String, Object> base, Map<String, Object> extra) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (base != null) {
            merged.putAll(base);
        }
        if (extra != null) {
            merged.putAll(extra);
        }
        return merged;
    }

    private void popScope(String scopeId) {
        Deque<ObservationScope> stack = ACTIVE_SCOPES.get();
        if (!stack.isEmpty() && scopeId.equals(stack.peek().getId())) {
            stack.pop();
            if (stack.isEmpty()) {
                ACTIVE_SCOPES.remove();
            }
            return;
        }
        stack.removeIf(scope -> scopeId.equals(scope.getId()));
        if (stack.isEmpty()) {
            ACTIVE_SCOPES.remove();
        }
    }

    private String summarize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() <= 400) {
            return normalized;
        }
        return normalized.substring(0, 397) + "...";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            return "{\"serializationError\":\"" + summarize(e.getMessage()) + "\"}";
        }
    }

    private String hex(int length) {
        byte[] bytes = new byte[length / 2];
        new SecureRandom().nextBytes(bytes);
        StringBuilder builder = new StringBuilder(length);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
