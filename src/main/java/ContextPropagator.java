import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextPropagator {
    public static String addContext(String message) {

        final Logger logger = LoggerFactory.getLogger(ContextPropagator.class);

        // Add context information to the message
        String context = "Kafka-Solace-Context: Propagated";
        logger.debug("Adding context: {}", context);
        String messageWithContext = context + "\n" + message;
        return context + "\n" + message;
    }
}
