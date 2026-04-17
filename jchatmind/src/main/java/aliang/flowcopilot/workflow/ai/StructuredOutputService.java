package aliang.flowcopilot.workflow.ai;

import aliang.flowcopilot.config.FlowCopilotObservabilityProperties;
import aliang.flowcopilot.workflow.agent.ToolRegistry;
import aliang.flowcopilot.workflow.agent.WorkflowAgentProfile;
import aliang.flowcopilot.workflow.observability.ObservationRecorder;
import aliang.flowcopilot.workflow.observability.ObservationScope;
import aliang.flowcopilot.workflow.observability.ObservationSpanType;
import aliang.flowcopilot.workflow.observability.ObservationStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates structured node output with LangChain4j and falls back to deterministic templates.
 */
@Slf4j
@Service
@AllArgsConstructor
public class StructuredOutputService {

    private final ChatModelProvider chatModelProvider;
    private final ToolRegistry toolRegistry;
    private final ObservationRecorder observationRecorder;
    private final FlowCopilotObservabilityProperties observabilityProperties;

    public String generateOrFallback(WorkflowAgentProfile profile, String taskPrompt, String fallback) {
        ObservationScope llmScope = observationRecorder.startChildSpan(
                ObservationSpanType.LLM_CALL,
                profile.getDisplayName() + " completion",
                null,
                taskPrompt,
                llmAttributes(profile)
        );
        try {
            String prompt = """
                    [Role Responsibility]
                    %s

                    [Allowed Tools]
                    %s

                    [Output Requirements]
                    Please respond in Markdown and include:
                    - Goal understanding
                    - Key supporting evidence
                    - Structured result

                    [Task]
                    %s
                    """.formatted(
                    profile.getResponsibility(),
                    toolRegistry.allowedTools(profile.getRole()),
                    taskPrompt
            );
            ChatCompletionResult completion = chatModelProvider.chat(profile.getSystemPrompt(), prompt);
            String result = completion.getText();
            String finalResult = result == null || result.isBlank() ? fallback : result.strip();
            observationRecorder.complete(
                    llmScope,
                    ObservationStatus.COMPLETED,
                    finalResult,
                    llmCompletionAttributes(completion, result == null || result.isBlank())
            );
            return finalResult;
        } catch (Exception e) {
            log.warn("{} fell back to deterministic output: {}", profile.getDisplayName(), e.getMessage());
            observationRecorder.fail(llmScope, e, Map.of("fallbackUsed", true));
            return fallback;
        }
    }

    private Map<String, Object> llmAttributes(WorkflowAgentProfile profile) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("agentRole", profile.getRole().name());
        attributes.put("displayName", profile.getDisplayName());
        attributes.put("allowedTools", String.join(",", toolRegistry.allowedTools(profile.getRole())));
        attributes.put("langsmith.span.kind", "llm");
        return attributes;
    }

    private Map<String, Object> llmCompletionAttributes(ChatCompletionResult completion, boolean fallbackUsed) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("fallbackUsed", fallbackUsed);
        putIfPresent(attributes, "modelName", completion.getModelName());
        putIfPresent(attributes, "responseId", completion.getResponseId());
        putIfPresent(attributes, "finishReason", completion.getFinishReason());
        putIfPresent(attributes, "gen_ai.request.model", completion.getModelName());
        putIfPresent(attributes, "gen_ai.response.model", completion.getModelName());
        putIfPresent(attributes, "gen_ai.response.id", completion.getResponseId());
        putIfPresent(attributes, "gen_ai.response.finish_reason", completion.getFinishReason());
        putIfPresent(attributes, "inputTokens", completion.getInputTokens());
        putIfPresent(attributes, "outputTokens", completion.getOutputTokens());
        putIfPresent(attributes, "totalTokens", completion.getTotalTokens());
        putIfPresent(attributes, "gen_ai.usage.input_tokens", completion.getInputTokens());
        putIfPresent(attributes, "gen_ai.usage.output_tokens", completion.getOutputTokens());
        putIfPresent(attributes, "gen_ai.usage.total_tokens", completion.getTotalTokens());
        BigDecimal estimatedCostUsd = estimateCostUsd(completion);
        if (estimatedCostUsd != null) {
            attributes.put("estimatedCostUsd", estimatedCostUsd);
        }
        return attributes;
    }

    private BigDecimal estimateCostUsd(ChatCompletionResult completion) {
        if (completion.getModelName() == null) {
            return null;
        }
        FlowCopilotObservabilityProperties.ModelPricing pricing = observabilityProperties.pricingForModel(completion.getModelName());
        if (pricing == null) {
            return null;
        }
        BigDecimal inputCost = prorate(pricing.getInputCostPerMillionUsd(), completion.getInputTokens());
        BigDecimal outputCost = prorate(pricing.getOutputCostPerMillionUsd(), completion.getOutputTokens());
        if (inputCost == null && outputCost == null) {
            return null;
        }
        return safe(inputCost).add(safe(outputCost)).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal prorate(BigDecimal perMillionUsd, Integer tokens) {
        if (perMillionUsd == null || tokens == null) {
            return null;
        }
        return perMillionUsd
                .multiply(BigDecimal.valueOf(tokens))
                .divide(BigDecimal.valueOf(1_000_000L), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void putIfPresent(Map<String, Object> attributes, String key, Object value) {
        if (value != null) {
            attributes.put(key, value);
        }
    }
}
