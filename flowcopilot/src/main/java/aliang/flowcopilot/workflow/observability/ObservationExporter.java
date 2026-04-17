package aliang.flowcopilot.workflow.observability;

import java.util.Map;

public interface ObservationExporter {

    ExportedSpanHandle start(ExportObservationRequest request, ExportedSpanHandle parentHandle);

    void complete(ExportedSpanHandle handle, String outputSummary, Map<String, Object> attributes);

    void fail(ExportedSpanHandle handle, Throwable error, Map<String, Object> attributes);

    boolean isEnabled();

    String provider();

    String status();

    String lastErrorMessage();

    String buildTraceUrl(String traceId);

    record ExportObservationRequest(
            String name,
            ObservationSpanType spanType,
            String traceId,
            String spanId,
            String inputSummary,
            Map<String, Object> attributes
    ) {
    }

    record ExportedSpanHandle(String traceId, String spanId, Object nativeHandle) {
    }
}
