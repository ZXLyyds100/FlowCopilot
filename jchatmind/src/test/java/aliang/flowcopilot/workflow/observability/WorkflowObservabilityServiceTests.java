package aliang.flowcopilot.workflow.observability;

import aliang.flowcopilot.config.FlowCopilotObservabilityProperties;
import aliang.flowcopilot.model.entity.ExecutionObservation;
import aliang.flowcopilot.model.response.GetWorkflowObservabilityResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkflowObservabilityServiceTests {

    @Test
    void shouldBuildHierarchicalSpanTreeAndSummary() {
        ObservationRecorder recorder = mock(ObservationRecorder.class);
        ObservationExporter exporter = mock(ObservationExporter.class);
        FlowCopilotObservabilityProperties properties = new FlowCopilotObservabilityProperties();
        properties.getLangsmith().setProject("flowcopilot-demo");
        WorkflowObservabilityService service = new WorkflowObservabilityService(recorder, properties, new ObjectMapper());

        List<ExecutionObservation> observations = List.of(
                observation("1", "trace-2", "wf-span", null, ObservationSpanType.WORKFLOW_RUN, null, "workflow", LocalDateTime.of(2026, 4, 16, 10, 0)),
                observation("2", "trace-2", "node-span", "wf-span", ObservationSpanType.NODE_RUN, "planner", "planner", LocalDateTime.of(2026, 4, 16, 10, 1)),
                observation("3", "trace-2", "llm-span", "node-span", ObservationSpanType.LLM_CALL, "planner", "llm", LocalDateTime.of(2026, 4, 16, 10, 2))
        );

        when(recorder.getObservations("wf-1")).thenReturn(observations);
        when(recorder.exporter()).thenReturn(exporter);
        when(exporter.isEnabled()).thenReturn(true);
        when(exporter.provider()).thenReturn("otel");
        when(exporter.status()).thenReturn("enabled");
        when(exporter.buildTraceUrl("trace-2")).thenReturn("https://trace.example/trace-2");

        GetWorkflowObservabilityResponse response = service.getObservability("wf-1");

        assertEquals(3, response.getSummary().getTotalSpans());
        assertEquals(1, response.getSummary().getWorkflowRuns());
        assertEquals(1, response.getSummary().getNodeRuns());
        assertEquals(1, response.getSummary().getLlmCalls());
        assertEquals(120, response.getSummary().getInputTokens());
        assertEquals(80, response.getSummary().getOutputTokens());
        assertEquals(200, response.getSummary().getTotalTokens());
        assertEquals(new BigDecimal("0.001234"), response.getSummary().getEstimatedCostUsd());
        assertEquals("enabled", response.getSummary().getExporterStatus());
        assertEquals("trace-2", response.getSummary().getLatestTraceId());
        assertEquals(1, response.getSpans().length);
        assertEquals("wf-span", response.getSpans()[0].getSpanId());
        assertEquals(1, response.getSpans()[0].getChildren().length);
        assertEquals("node-span", response.getSpans()[0].getChildren()[0].getSpanId());
        assertEquals(1, response.getSpans()[0].getChildren()[0].getChildren().length);
        assertEquals("llm-span", response.getSpans()[0].getChildren()[0].getChildren()[0].getSpanId());
        assertEquals("deepseek-chat", response.getSpans()[0].getChildren()[0].getChildren()[0].getModelName());
        assertEquals(200, response.getSpans()[0].getChildren()[0].getChildren()[0].getTotalTokens());
        assertTrue(response.getExternalTrace().isEnabled());
        assertEquals("flowcopilot-demo", response.getExternalTrace().getProjectName());
        assertEquals("https://trace.example/trace-2", response.getExternalTrace().getUrl());
    }

    @Test
    void shouldReturnNotInstrumentedStateForHistoricalWorkflowWithoutObservations() {
        ObservationRecorder recorder = mock(ObservationRecorder.class);
        ObservationExporter exporter = mock(ObservationExporter.class);
        WorkflowObservabilityService service = new WorkflowObservabilityService(
                recorder,
                new FlowCopilotObservabilityProperties(),
                new ObjectMapper()
        );

        when(recorder.getObservations("wf-history")).thenReturn(List.of());
        when(recorder.exporter()).thenReturn(exporter);
        when(exporter.isEnabled()).thenReturn(false);
        when(exporter.provider()).thenReturn("local");

        GetWorkflowObservabilityResponse response = service.getObservability("wf-history");

        assertEquals(0, response.getSummary().getTotalSpans());
        assertEquals("not_instrumented", response.getSummary().getExporterStatus());
        assertEquals("not_instrumented", response.getExternalTrace().getStatus());
        assertFalse(response.getExternalTrace().isEnabled());
        assertEquals(0, response.getSpans().length);
    }

    private static ExecutionObservation observation(String id,
                                                    String traceId,
                                                    String spanId,
                                                    String parentSpanId,
                                                    ObservationSpanType spanType,
                                                    String nodeKey,
                                                    String name,
                                                    LocalDateTime startedAt) {
        return ExecutionObservation.builder()
                .id(id)
                .runId("run-1")
                .traceId(traceId)
                .spanId(spanId)
                .parentSpanId(parentSpanId)
                .workflowInstanceId("wf-1")
                .nodeKey(nodeKey)
                .spanType(spanType.name())
                .name(name)
                .status(ObservationStatus.COMPLETED.name())
                .startedAt(startedAt)
                .endedAt(startedAt.plusSeconds(1))
                .durationMs(1000L)
                .attributesJson(spanType == ObservationSpanType.LLM_CALL
                        ? "{\"modelName\":\"deepseek-chat\",\"inputTokens\":120,\"outputTokens\":80,\"totalTokens\":200,\"estimatedCostUsd\":0.001234}"
                        : "{}")
                .build();
    }
}
