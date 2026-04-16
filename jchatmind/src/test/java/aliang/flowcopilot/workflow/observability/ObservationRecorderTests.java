package aliang.flowcopilot.workflow.observability;

import aliang.flowcopilot.config.FlowCopilotObservabilityProperties;
import aliang.flowcopilot.model.entity.ExecutionObservation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ObservationRecorderTests {

    @Test
    void shouldBuildParentChildRelationshipsAndPersistSpanTypes() {
        InMemoryObservationStore store = new InMemoryObservationStore();
        ObservationRecorder recorder = new ObservationRecorder(
                store,
                new StableObservationExporter(),
                enabledProperties(),
                new ObjectMapper()
        );

        String workflowInstanceId = "11111111-1111-1111-1111-111111111111";
        String traceId = recorder.nextTraceId();
        String runId = recorder.nextRunId();

        ObservationScope workflowScope = recorder.startWorkflowRun(
                workflowInstanceId,
                traceId,
                runId,
                "workflow-run",
                "planner",
                "workflow input",
                Map.of("reason", "create")
        );
        ObservationScope nodeScope = recorder.startNodeRun(
                workflowInstanceId,
                runId,
                "planner",
                "planner-node",
                "node input",
                Map.of("nodeOrder", 1)
        );
        ObservationScope llmScope = recorder.startChildSpan(
                ObservationSpanType.LLM_CALL,
                "planner-llm",
                "planner",
                "prompt",
                Map.of("model", "deepseek-chat")
        );

        recorder.complete(llmScope, ObservationStatus.COMPLETED, "llm output", Map.of("tokens", 128));
        recorder.complete(nodeScope, ObservationStatus.COMPLETED, "node output", Map.of("status", "ok"));
        recorder.complete(workflowScope, ObservationStatus.COMPLETED, "workflow output", Map.of("finished", true));

        List<ExecutionObservation> observations = recorder.getObservations(workflowInstanceId);
        assertEquals(3, observations.size());

        ExecutionObservation workflowObservation = findByType(observations, ObservationSpanType.WORKFLOW_RUN);
        ExecutionObservation nodeObservation = findByType(observations, ObservationSpanType.NODE_RUN);
        ExecutionObservation llmObservation = findByType(observations, ObservationSpanType.LLM_CALL);

        assertEquals(workflowObservation.getSpanId(), nodeObservation.getParentSpanId());
        assertEquals(nodeObservation.getSpanId(), llmObservation.getParentSpanId());
        assertEquals(ObservationStatus.COMPLETED.name(), workflowObservation.getStatus());
        assertEquals(ObservationStatus.COMPLETED.name(), nodeObservation.getStatus());
        assertEquals(ObservationStatus.COMPLETED.name(), llmObservation.getStatus());
        assertNotNull(llmObservation.getDurationMs());
    }

    @Test
    void shouldIgnoreExporterFailuresDuringCompleteAndFail() {
        InMemoryObservationStore store = new InMemoryObservationStore();
        ObservationRecorder recorder = new ObservationRecorder(
                store,
                new FailingObservationExporter(),
                enabledProperties(),
                new ObjectMapper()
        );

        String workflowInstanceId = "22222222-2222-2222-2222-222222222222";

        ObservationScope completedScope = recorder.startWorkflowRun(
                workflowInstanceId,
                recorder.nextTraceId(),
                recorder.nextRunId(),
                "completed-run",
                "planner",
                "input",
                Map.of()
        );
        assertDoesNotThrow(() ->
                recorder.complete(completedScope, ObservationStatus.COMPLETED, "done", Map.of("exported", false))
        );

        ObservationScope failedScope = recorder.startWorkflowRun(
                workflowInstanceId,
                recorder.nextTraceId(),
                recorder.nextRunId(),
                "failed-run",
                "planner",
                "input",
                Map.of()
        );
        assertDoesNotThrow(() ->
                recorder.fail(failedScope, new IllegalStateException("export failure"), Map.of("exported", false))
        );

        List<ExecutionObservation> observations = recorder.getObservations(workflowInstanceId);
        assertEquals(2, observations.size());
        assertEquals(1L, observations.stream().filter(item -> ObservationStatus.COMPLETED.name().equals(item.getStatus())).count());
        assertEquals(1L, observations.stream().filter(item -> ObservationStatus.FAILED.name().equals(item.getStatus())).count());
    }

    private static FlowCopilotObservabilityProperties enabledProperties() {
        FlowCopilotObservabilityProperties properties = new FlowCopilotObservabilityProperties();
        properties.setEnabled(true);
        properties.setPersistEnabled(true);
        properties.setOtelEnabled(false);
        return properties;
    }

    private static ExecutionObservation findByType(List<ExecutionObservation> observations, ObservationSpanType spanType) {
        return observations.stream()
                .filter(item -> spanType.name().equals(item.getSpanType()))
                .findFirst()
                .orElseThrow();
    }

    private static final class InMemoryObservationStore implements ObservationStore {
        private final List<ExecutionObservation> observations = new ArrayList<>();

        @Override
        public void create(ExecutionObservation observation) {
            observations.add(observation);
        }

        @Override
        public void update(ExecutionObservation observation) {
            observations.stream()
                    .filter(existing -> existing.getId().equals(observation.getId()))
                    .findFirst()
                    .ifPresent(existing -> {
                        existing.setStatus(observation.getStatus());
                        existing.setOutputSummary(observation.getOutputSummary());
                        existing.setErrorMessage(observation.getErrorMessage());
                        existing.setAttributesJson(observation.getAttributesJson());
                        existing.setEndedAt(observation.getEndedAt());
                        existing.setDurationMs(observation.getDurationMs());
                        existing.setUpdatedAt(LocalDateTime.now());
                    });
        }

        @Override
        public List<ExecutionObservation> findByWorkflowInstanceId(String workflowInstanceId) {
            return observations.stream()
                    .filter(item -> workflowInstanceId.equals(item.getWorkflowInstanceId()))
                    .sorted(Comparator.comparing(ExecutionObservation::getStartedAt))
                    .toList();
        }
    }

    private static class StableObservationExporter implements ObservationExporter {
        @Override
        public ExportedSpanHandle start(ExportObservationRequest request, ExportedSpanHandle parentHandle) {
            return new ExportedSpanHandle(request.traceId(), request.spanId(), request.name());
        }

        @Override
        public void complete(ExportedSpanHandle handle, String outputSummary, Map<String, Object> attributes) {
            // no-op
        }

        @Override
        public void fail(ExportedSpanHandle handle, Throwable error, Map<String, Object> attributes) {
            // no-op
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public String provider() {
            return "local";
        }

        @Override
        public String status() {
            return "disabled";
        }

        @Override
        public String lastErrorMessage() {
            return null;
        }

        @Override
        public String buildTraceUrl(String traceId) {
            return null;
        }
    }

    private static final class FailingObservationExporter extends StableObservationExporter {
        @Override
        public void complete(ExportedSpanHandle handle, String outputSummary, Map<String, Object> attributes) {
            throw new IllegalStateException("complete exporter unavailable");
        }

        @Override
        public void fail(ExportedSpanHandle handle, Throwable error, Map<String, Object> attributes) {
            throw new IllegalStateException("fail exporter unavailable");
        }
    }
}
