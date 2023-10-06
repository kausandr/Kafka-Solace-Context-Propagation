import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextPropagator {
    private static final Logger logger = LoggerFactory.getLogger(ContextPropagator.class);
    private static final Tracer tracer = GlobalOpenTelemetry.getTracer("KafkaSolaceIntegration");
    public static String addContext(String message) {
        //Create a unique span for each message
        Span span = tracer.spanBuilder("MessageProcessing").startSpan();
        try{
        // Add context information to the message
        String context = "Kafka-Solace-Context: Propagated";
        logger.debug("Adding context: {}", context);
        String messageWithContext = context + "\n" + message;

        //Add attributes to the span
            span.setAttribute("message", messageWithContext);

         //Simulate processing time (for demonstration purposes)
         Thread.sleep(100);

        return messageWithContext;
        } catch (InterruptedException e) {
            logger.error("Error while processing message: {}", e.getMessage());
            return message;
        } finally {
            //End span
            span.end();
        }
    }
}
