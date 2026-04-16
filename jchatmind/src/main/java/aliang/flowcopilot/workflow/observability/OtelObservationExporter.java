package aliang.flowcopilot.workflow.observability;

import aliang.flowcopilot.config.FlowCopilotObservabilityProperties;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class OtelObservationExporter implements ObservationExporter {

    private final FlowCopilotObservabilityProperties properties;
    private final Tracer tracer;
    private final ConcurrentMap<String, Span> activeSpans = new ConcurrentHashMap<>();
    private final AtomicReference<String> lastError = new AtomicReference<>();

    public OtelObservationExporter(FlowCopilotObservabilityProperties properties) {
        this.properties = properties;
        var exporterBuilder = OtlpHttpSpanExporter.builder()
                .setTimeout(properties.getOtelTimeout());
        if (properties.resolvedOtelEndpoint() != null && !properties.resolvedOtelEndpoint().isBlank()) {
            exporterBuilder.setEndpoint(properties.resolvedOtelEndpoint());
        }
        properties.resolvedOtelHeaders().forEach(exporterBuilder::addHeader);
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(Resource.getDefault().toBuilder()
                        .put("service.name", properties.getServiceName())
                        .build())
                .addSpanProcessor(BatchSpanProcessor.builder(exporterBuilder.build()).build())
                .build();
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
        this.tracer = openTelemetry.getTracer(properties.getServiceName());
    }

    @Override
    public ExportedSpanHandle start(ExportObservationRequest request, ExportedSpanHandle parentHandle) {
        try {
            io.opentelemetry.api.trace.SpanBuilder builder = tracer.spanBuilder(request.name());
            if (parentHandle != null && parentHandle.nativeHandle() instanceof Span parentSpan) {
                builder.setParent(Context.root().with(parentSpan));
            } else {
                builder.setNoParent();
            }
            Span span = builder.startSpan();
            span.setAttribute("flowcopilot.span_type", request.spanType().name());
            span.setAttribute("langsmith.span.kind", defaultLangsmithSpanKind(request.spanType()));
            if (request.inputSummary() != null) {
                span.setAttribute("flowcopilot.input_summary", request.inputSummary());
            }
            request.attributes().forEach((key, value) -> setAttribute(span, key, value));
            activeSpans.put(span.getSpanContext().getSpanId(), span);
            return new ExportedSpanHandle(
                    span.getSpanContext().getTraceId(),
                    span.getSpanContext().getSpanId(),
                    span
            );
        } catch (Exception e) {
            lastError.set(e.getMessage());
            return new ExportedSpanHandle(request.traceId(), request.spanId(), null);
        }
    }

    @Override
    public void complete(ExportedSpanHandle handle, String outputSummary, Map<String, Object> attributes) {
        Span span = activeSpans.remove(handle.spanId());
        if (span == null) {
            return;
        }
        try {
            if (outputSummary != null) {
                span.setAttribute("flowcopilot.output_summary", outputSummary);
            }
            attributes.forEach((key, value) -> setAttribute(span, key, value));
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            lastError.set(e.getMessage());
        } finally {
            span.end();
        }
    }

    @Override
    public void fail(ExportedSpanHandle handle, Throwable error, Map<String, Object> attributes) {
        Span span = activeSpans.remove(handle.spanId());
        if (span == null) {
            return;
        }
        try {
            if (error != null) {
                span.recordException(error);
                span.setStatus(StatusCode.ERROR, Objects.toString(error.getMessage(), error.getClass().getSimpleName()));
            } else {
                span.setStatus(StatusCode.ERROR);
            }
            attributes.forEach((key, value) -> setAttribute(span, key, value));
        } catch (Exception e) {
            lastError.set(e.getMessage());
        } finally {
            span.end();
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String provider() {
        return properties.isLangsmithEnabled() ? "langsmith" : "otel";
    }

    @Override
    public String status() {
        return lastError.get() == null ? "enabled" : "error";
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

    private void setAttribute(Span span, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Boolean booleanValue) {
            span.setAttribute(AttributeKey.booleanKey(key), booleanValue);
            return;
        }
        if (value instanceof Long longValue) {
            span.setAttribute(AttributeKey.longKey(key), longValue);
            return;
        }
        if (value instanceof Integer intValue) {
            span.setAttribute(AttributeKey.longKey(key), (long) intValue);
            return;
        }
        if (value instanceof Double doubleValue) {
            span.setAttribute(AttributeKey.doubleKey(key), doubleValue);
            return;
        }
        span.setAttribute(AttributeKey.stringKey(key), String.valueOf(value));
    }

    private String defaultLangsmithSpanKind(ObservationSpanType spanType) {
        return switch (spanType) {
            case WORKFLOW_RUN, NODE_RUN -> "chain";
            case LLM_CALL -> "llm";
            case TOOL_CALL -> "tool";
            case RETRIEVAL_CALL -> "retriever";
        };
    }
}
