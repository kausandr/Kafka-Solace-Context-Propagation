import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextPropagator {
    private static final Logger logger = LoggerFactory.getLogger(ContextPropagator.class);
    public static String addContext(String message) {


            // Add context information to the message
            String context = "Kafka-Solace-Context: Propagated";
            logger.debug("Adding context: {}", context);
            String messageWithContext = context + "\n" + message;

            return messageWithContext;
    }
}
