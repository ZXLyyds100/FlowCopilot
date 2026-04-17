package aliang.flowcopilot.workflow.observability;

import aliang.flowcopilot.config.FlowCopilotObservabilityProperties;
import aliang.flowcopilot.model.entity.ExecutionObservation;
import aliang.flowcopilot.model.response.GetWorkflowObservabilityResponse;
import aliang.flowcopilot.model.vo.WorkflowObservationExternalTraceVO;
import aliang.flowcopilot.model.vo.WorkflowObservationSpanVO;
import aliang.flowcopilot.model.vo.WorkflowObservationSummaryVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds workflow observability summaries and span trees for API responses.
 */
@Service
@AllArgsConstructor
public class WorkflowObservabilityService {

    private static final String NOT_INSTRUMENTED = "not_instrumented";

    private final ObservationRecorder observationRecorder;
    private final FlowCopilotObservabilityProperties observabilityProperties;
    private final ObjectMapper objectMapper;

    public GetWorkflowObservabilityResponse getObservability(String workflowInstanceId) {
        List<ExecutionObservation> observations = observationRecorder.getObservations(workflowInstanceId);
        List<WorkflowObservationSpanVO> spanTree = buildTree(observations);
        String latestTraceId = observations.stream()
                .max(Comparator.comparing(ExecutionObservation::getStartedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ExecutionObservation::getTraceId)
                .orElse(null);
        boolean instrumented = !observations.isEmpty();
        return GetWorkflowObservabilityResponse.builder()
                .workflowInstanceId(workflowInstanceId)
                .summary(buildSummary(observations, latestTraceId, instrumented))
                .spans(spanTree.toArray(WorkflowObservationSpanVO[]::new))
                .externalTrace(buildExternalTrace(latestTraceId, instrumented))
                .build();
    }

    private WorkflowObservationSummaryVO buildSummary(List<ExecutionObservation> observations,
                                                     String latestTraceId,
                                                     boolean instrumented) {
        UsageTotals usageTotals = aggregateUsage(observations);
        return WorkflowObservationSummaryVO.builder()
                .totalSpans(observations.size())
                .workflowRuns(countType(observations, ObservationSpanType.WORKFLOW_RUN))
                .nodeRuns(countType(observations, ObservationSpanType.NODE_RUN))
                .llmCalls(countType(observations, ObservationSpanType.LLM_CALL))
                .toolCalls(countType(observations, ObservationSpanType.TOOL_CALL))
                .retrievalCalls(countType(observations, ObservationSpanType.RETRIEVAL_CALL))
                .errorCount((int) observations.stream().filter(item -> ObservationStatus.FAILED.name().equals(item.getStatus())).count())
                .inputTokens(usageTotals.inputTokens())
                .outputTokens(usageTotals.outputTokens())
                .totalTokens(usageTotals.totalTokens())
                .estimatedCostUsd(usageTotals.estimatedCostUsd())
                .exporterStatus(instrumented ? observationRecorder.exporter().status() : NOT_INSTRUMENTED)
                .latestTraceId(latestTraceId)
                .build();
    }

    private WorkflowObservationExternalTraceVO buildExternalTrace(String latestTraceId, boolean instrumented) {
        ObservationExporter exporter = observationRecorder.exporter();
        return WorkflowObservationExternalTraceVO.builder()
                .enabled(exporter.isEnabled())
                .provider(exporter.provider())
                .status(instrumented ? exporter.status() : NOT_INSTRUMENTED)
                .traceId(latestTraceId)
                .projectName(observabilityProperties.getLangsmith().getProject())
                .url(instrumented ? exporter.buildTraceUrl(latestTraceId) : null)
                .lastErrorMessage(instrumented ? exporter.lastErrorMessage() : null)
                .build();
    }

    private int countType(List<ExecutionObservation> observations, ObservationSpanType spanType) {
        return (int) observations.stream()
                .filter(item -> spanType.name().equalsIgnoreCase(item.getSpanType()))
                .count();
    }

    private List<WorkflowObservationSpanVO> buildTree(List<ExecutionObservation> observations) {
        Map<String, WorkflowObservationSpanVO> nodesBySpanId = new LinkedHashMap<>();
        observations.stream()
                .sorted(Comparator.comparing(ExecutionObservation::getStartedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(observation -> nodesBySpanId.put(observation.getSpanId(), toVO(observation)));
        List<WorkflowObservationSpanVO> roots = new ArrayList<>();
        Map<String, List<WorkflowObservationSpanVO>> childrenByParent = new LinkedHashMap<>();
        nodesBySpanId.values().forEach(node -> {
            if (node.getParentSpanId() == null || !nodesBySpanId.containsKey(node.getParentSpanId())) {
                roots.add(node);
                return;
            }
            childrenByParent.computeIfAbsent(node.getParentSpanId(), ignored -> new ArrayList<>()).add(node);
        });
        nodesBySpanId.values().forEach(node -> node.setChildren(
                childrenByParent.getOrDefault(node.getSpanId(), List.of())
                        .stream()
                        .sorted(Comparator.comparing(WorkflowObservationSpanVO::getStartedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                        .toArray(WorkflowObservationSpanVO[]::new)
        ));
        roots.sort(Comparator.comparing(WorkflowObservationSpanVO::getStartedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return roots;
    }

    private WorkflowObservationSpanVO toVO(ExecutionObservation observation) {
        JsonNode attributes = parseAttributes(observation.getAttributesJson());
        return WorkflowObservationSpanVO.builder()
                .id(observation.getId())
                .runId(observation.getRunId())
                .traceId(observation.getTraceId())
                .spanId(observation.getSpanId())
                .parentSpanId(observation.getParentSpanId())
                .workflowInstanceId(observation.getWorkflowInstanceId())
                .nodeKey(observation.getNodeKey())
                .spanType(observation.getSpanType())
                .name(observation.getName())
                .status(observation.getStatus())
                .inputSummary(observation.getInputSummary())
                .outputSummary(observation.getOutputSummary())
                .errorMessage(observation.getErrorMessage())
                .attributesJson(observation.getAttributesJson())
                .modelName(textValue(attributes, "modelName", "gen_ai.response.model", "gen_ai.request.model"))
                .responseId(textValue(attributes, "responseId", "gen_ai.response.id"))
                .finishReason(textValue(attributes, "finishReason", "gen_ai.response.finish_reason"))
                .inputTokens(intValue(attributes, "inputTokens", "gen_ai.usage.input_tokens"))
                .outputTokens(intValue(attributes, "outputTokens", "gen_ai.usage.output_tokens"))
                .totalTokens(intValue(attributes, "totalTokens", "gen_ai.usage.total_tokens"))
                .estimatedCostUsd(decimalValue(attributes, "estimatedCostUsd"))
                .startedAt(observation.getStartedAt())
                .endedAt(observation.getEndedAt())
                .durationMs(observation.getDurationMs())
                .children(new WorkflowObservationSpanVO[0])
                .build();
    }

    private UsageTotals aggregateUsage(List<ExecutionObservation> observations) {
        int inputTokens = 0;
        int outputTokens = 0;
        int totalTokens = 0;
        BigDecimal estimatedCostUsd = BigDecimal.ZERO;
        boolean hasUsage = false;
        boolean hasCost = false;

        for (ExecutionObservation observation : observations) {
            JsonNode attributes = parseAttributes(observation.getAttributesJson());
            Integer input = intValue(attributes, "inputTokens", "gen_ai.usage.input_tokens");
            Integer output = intValue(attributes, "outputTokens", "gen_ai.usage.output_tokens");
            Integer total = intValue(attributes, "totalTokens", "gen_ai.usage.total_tokens");
            BigDecimal cost = decimalValue(attributes, "estimatedCostUsd");

            if (input != null) {
                inputTokens += input;
                hasUsage = true;
            }
            if (output != null) {
                outputTokens += output;
                hasUsage = true;
            }
            if (total != null) {
                totalTokens += total;
                hasUsage = true;
            }
            if (cost != null) {
                estimatedCostUsd = estimatedCostUsd.add(cost);
                hasCost = true;
            }
        }

        return new UsageTotals(
                hasUsage ? inputTokens : null,
                hasUsage ? outputTokens : null,
                hasUsage ? totalTokens : null,
                hasCost ? estimatedCostUsd : null
        );
    }

    private JsonNode parseAttributes(String attributesJson) {
        if (attributesJson == null || attributesJson.isBlank()) {
            return objectMapper.nullNode();
        }
        try {
            return objectMapper.readTree(attributesJson);
        } catch (Exception ignored) {
            return objectMapper.nullNode();
        }
    }

    private String textValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull()) {
                String text = value.asText();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private Integer intValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull()) {
                try {
                    return value.isNumber() ? value.intValue() : Integer.valueOf(value.asText());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private BigDecimal decimalValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull()) {
                try {
                    return value.isNumber() ? value.decimalValue() : new BigDecimal(value.asText());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private record UsageTotals(
            Integer inputTokens,
            Integer outputTokens,
            Integer totalTokens,
            BigDecimal estimatedCostUsd
    ) {
    }
}
