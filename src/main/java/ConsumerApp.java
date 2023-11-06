
import com.solacesystems.jcsmp.*;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

public class ConsumerApp {
    public static void main(String[] args) {

        final Logger logger = LoggerFactory.getLogger(ConsumerApp.class);

        String kafkaTopic = "kafkatest";
        String solaceTopic = "solacetest";
        String kafkaBootstrapServers = "tcp://kafka-ps:9092";
        String solaceHost = "tcp://solace-ps:55555";
        String solaceVPN = "default";
        String solaceUsername = "admin";  //username and password for solace UI dashboard
        String solacePassword = "admin";

        //setting up kafka consumer properties
        Properties kafkaProps = new Properties();
        kafkaProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        kafkaProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        kafkaProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        kafkaProps.put(ConsumerConfig.GROUP_ID_CONFIG, "group1");

        //setting up kafka consumer to consume messages
        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(kafkaProps);
        kafkaConsumer.subscribe(Collections.singletonList(kafkaTopic));

        //setting up solace context
        JCSMPProperties solaceProps = new JCSMPProperties();
        solaceProps.setProperty(JCSMPProperties.HOST, solaceHost);
        solaceProps.setProperty(JCSMPProperties.VPN_NAME, solaceVPN);
        solaceProps.setProperty(JCSMPProperties.USERNAME, solaceUsername);
        solaceProps.setProperty(JCSMPProperties.PASSWORD, solacePassword);

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

                    System.out.println("<<=== Consumer record from topic" + record.topic() + ", partition " + record.partition() + ", offset "
                            + + record.offset() +" message:" + ContextPropagator.addContext(kafkaMessage));

                    //Add context information
                    String solaceMessage = ContextPropagator.addContext(kafkaMessage);

                    //Publish to Solace
                    TextMessage solaceTextMsg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
                    solaceTextMsg.setText(solaceMessage);
                    solaceProducer.send(solaceTextMsg, JCSMPFactory.onlyInstance().createTopic(solaceTopic));
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