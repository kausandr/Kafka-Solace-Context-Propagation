import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.autoconfigure.ConfigProperties;


public class OpenTelemetryConfig {
    public static void configure(){
        ConfigProperties config = ConfigProperties.get();
        SpanExporter jaegerExporter = configureJaegerExporter(config);

        OpenTelemetrySdk.builder().setPropagators(ContextPropagator.create()).setTracerProvider(
                ConfigurableOpenTelemetryProvider.builder()
                        .addSpanProcessor(createSpanprocessor(jaegerExporter)).build()
        ).buildAndRegisterGlobal();
    }

    private static SpanExporter configureJaegerExporter(ConfigProperties config) {
        String jaegerEndpoint = config.getString("jaeger-all-in-one:14250");
        JaegerGrpcSpanExporter exporter = JaegerGrpcSpanExporter.builder().setEndpoint(jaegerEndpoint).build();
        return exporter;
    }
    private static SpanProcessor createSpanProcessor(SpanExporter exporter){
        return SimpleSpanProcessor.builder(exporter).build();
    }
}