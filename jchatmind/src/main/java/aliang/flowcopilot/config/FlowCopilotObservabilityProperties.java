package aliang.flowcopilot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Observability properties for local persistence and optional OTEL export.
 */
@Data
@ConfigurationProperties(prefix = "flowcopilot.observability")
public class FlowCopilotObservabilityProperties {

    private boolean enabled = true;
    private boolean persistEnabled = true;
    private boolean otelEnabled = false;
    private String serviceName = "flowcopilot";
    private String externalTraceBaseUrl;
    private String otelEndpoint;
    private Duration otelTimeout = Duration.ofSeconds(10);
    private Map<String, String> otelHeaders = new LinkedHashMap<>();
    private LangsmithProperties langsmith = new LangsmithProperties();
    private PricingProperties pricing = new PricingProperties();

    public boolean shouldExportToOtel() {
        return enabled && (otelEnabled || isLangsmithEnabled());
    }

    public boolean isLangsmithEnabled() {
        return langsmith != null && langsmith.isEnabled() && hasText(langsmith.getApiKey());
    }

    public String resolvedOtelEndpoint() {
        if (isLangsmithEnabled() && hasText(langsmith.getEndpoint())) {
            return langsmith.getEndpoint();
        }
        return otelEndpoint;
    }

    public Map<String, String> resolvedOtelHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        otelHeaders.forEach((key, value) -> {
            if (hasText(key) && hasText(value)) {
                headers.put(key, value);
            }
        });
        if (isLangsmithEnabled()) {
            headers.putIfAbsent("x-api-key", langsmith.getApiKey());
            if (hasText(langsmith.getProject())) {
                headers.putIfAbsent("langsmith-project", langsmith.getProject());
            }
        }
        return headers;
    }

    public ModelPricing pricingForModel(String modelName) {
        if (pricing == null || !pricing.isEnabled() || !hasText(modelName)) {
            return null;
        }
        ModelPricing direct = pricing.getModels().get(modelName);
        if (direct != null) {
            return direct;
        }
        return pricing.getModels().entrySet().stream()
                .filter(entry -> modelName.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Data
    public static class LangsmithProperties {
        private boolean enabled = false;
        private String endpoint = "https://api.smith.langchain.com/otel/v1/traces";
        private String apiKey;
        private String project;
    }

    @Data
    public static class PricingProperties {
        private boolean enabled = false;
        private Map<String, ModelPricing> models = new LinkedHashMap<>();
    }

    @Data
    public static class ModelPricing {
        private BigDecimal inputCostPerMillionUsd;
        private BigDecimal outputCostPerMillionUsd;
    }
}
