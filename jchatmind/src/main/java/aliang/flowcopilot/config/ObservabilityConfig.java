package aliang.flowcopilot.config;

import aliang.flowcopilot.workflow.observability.NoOpObservationExporter;
import aliang.flowcopilot.workflow.observability.ObservationExporter;
import aliang.flowcopilot.workflow.observability.OtelObservationExporter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring for workflow observability services.
 */
@Configuration
@EnableConfigurationProperties(FlowCopilotObservabilityProperties.class)
public class ObservabilityConfig {

    @Bean
    public ObservationExporter observationExporter(FlowCopilotObservabilityProperties properties) {
        if (!properties.shouldExportToOtel()) {
            return new NoOpObservationExporter(properties);
        }
        return new OtelObservationExporter(properties);
    }
}
