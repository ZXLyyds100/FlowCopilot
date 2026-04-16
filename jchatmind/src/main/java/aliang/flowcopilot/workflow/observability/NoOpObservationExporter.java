package aliang.flowcopilot.workflow.observability;

import aliang.flowcopilot.config.FlowCopilotObservabilityProperties;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class NoOpObservationExporter implements ObservationExporter {

    private final FlowCopilotObservabilityProperties properties;
    private final AtomicReference<String> lastError = new AtomicReference<>();

    public NoOpObservationExporter(FlowCopilotObservabilityProperties properties) {
        this.properties = properties;
    }

    @Override
    public ExportedSpanHandle start(ExportObservationRequest request, ExportedSpanHandle parentHandle) {
        return new ExportedSpanHandle(request.traceId(), request.spanId(), null);
    }

    @Override
    public void complete(ExportedSpanHandle handle, String outputSummary, Map<String, Object> attributes) {
        // no-op
    }

    @Override
    public void fail(ExportedSpanHandle handle, Throwable error, Map<String, Object> attributes) {
        if (error != null) {
            lastError.set(error.getMessage());
        }
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
        return lastError.get();
    }

    @Override
    public String buildTraceUrl(String traceId) {
        if (traceId == null || properties.getExternalTraceBaseUrl() == null || properties.getExternalTraceBaseUrl().isBlank()) {
            return null;
        }
        return properties.getExternalTraceBaseUrl().endsWith("/")
                ? properties.getExternalTraceBaseUrl() + traceId
                : properties.getExternalTraceBaseUrl() + "/" + traceId;
    }
}
