
import com.solacesystems.jcsmp.*;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerBuilder;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

public class ConsumerApp {
    private static Tracer tracer;

    public ConsumerApp(OpenTelemetry openTelemetry){
        tracer = openTelemetry.getTracer("io.opentelemetry.example.JaegerExample");
    }

    public static void main(String[] args) {

        final Logger logger = LoggerFactory.getLogger(ConsumerApp.class);

        String kafkaTopic = "kafkatest";
        String solaceTopic = "solacetest";
        String kafkaBootstrapServers = "tcp://kafka-ps:9092";
        String solaceHost = "tcp://solace-ps:55555";
        String solaceVPN = "default";
        String solaceUsername = "admin";
        String solacePassword = "admin";

        //setting up kafka consumer properties
        Properties kafkaProps = new Properties();
        kafkaProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        kafkaProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        kafkaProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        kafkaProps.put(ConsumerConfig.GROUP_ID_CONFIG, "group1");

        KafkaConsumer<String,String> kafkaConsumer = new KafkaConsumer<>(kafkaProps);
        kafkaConsumer.subscribe(Collections.singletonList(kafkaTopic));

        //setting up solace context
        JCSMPProperties solaceProps = new JCSMPProperties();
        solaceProps.setProperty(JCSMPProperties.HOST, solaceHost);
        solaceProps.setProperty(JCSMPProperties.VPN_NAME, solaceVPN);
        solaceProps.setProperty(JCSMPProperties.USERNAME, solaceUsername);
        solaceProps.setProperty(JCSMPProperties.PASSWORD, solacePassword);

        //Initialize the Jaeger exporter
        JaegerGrpcSpanExporter jaegerExporter = JaegerGrpcSpanExporter.builder()
                .setEndpoint("http://jaeger-collector:14268/api/traces")
                .build();

        //Create an OpenTelemetry Tracer
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(jaegerExporter).build())
                .build();

        //Create an OpenTelemetry instance
        io.opentelemetry.api.OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
        //Set the global OpenTelemetry instance
        GlobalOpenTelemetry.set(openTelemetry);

        XMLMessageProducer solaceProducer = null;
        try {
            JCSMPSession solaceSession = JCSMPFactory.onlyInstance().createSession(solaceProps);
            solaceSession.connect();

            solaceProducer = solaceSession.getMessageProducer(new JCSMPStreamingPublishEventHandler() {
                @Override
                public void responseReceived(String messageId) {
                    logger.info("Solace message sent successfully with ID: {}", messageId);
                }

                @Override
                public void handleError(String messageId, JCSMPException e, long timestamp) {
                    logger.error("Error sending Solace message with ID {}: {}", messageId, e);
                }
            });

            while (true) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    // Extract Kafka message
                    String kafkaMessage = record.value();

                    //Create a span for Kafka processing
                    Span kafkaSpan = tracer.spanBuilder("Kafka Processing").startSpan();
                    try (Scope scope = kafkaSpan.makeCurrent()){
                        //Add attributes to the Kafka span
                        kafkaSpan.setAttribute("kafkatest",kafkaTopic);

                        //Add context information
                        String solaceMessage = ContextPropagator.addContext(kafkaMessage);

                        //Create a span for Solace processing
                        Span solaceSpan = tracer.spanBuilder("Solace Processing").startSpan();
                        try (Scope solaceScope = solaceSpan.makeCurrent()) {
                            //Add attributes to the Solace span
                            solaceSpan.setAttribute("solacetest", solaceTopic);

                            //Publish to Solace
                            TextMessage solaceTextMsg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
                            solaceTextMsg.setText(solaceMessage);
                            solaceProducer.send(solaceTextMsg, JCSMPFactory.onlyInstance().createTopic(solaceTopic));
                        } finally {
                            solaceSpan.end();
                        }
                    }finally {
                        kafkaSpan.end();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in Kafka-Solace Context Propagation Application: {}", e);
        } finally {
            if (solaceProducer != null) {
                solaceProducer.close();
            }
            kafkaConsumer.close();
        }
    }
}