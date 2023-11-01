import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.KafkaTelemetry;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.TracingProducerInterceptor;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.sql.Time;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class KafkaProducerApp {
    private static boolean produceMessages = true;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        produceMessages = false;
                    }
                })
        );

        String kafkaBootstrapServers = "tcp://kafka-ps:9092";
        String kafkaTopic = "kafkatest";

        Properties producerProp = new Properties();
        producerProp.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        producerProp.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProp.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
//        producerProp.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingProducerInterceptor.class.getName());

        Producer<String, String> producer = new KafkaProducer<>(producerProp);
        KafkaTelemetry telemetry = KafkaTelemetry.create(GlobalOpenTelemetry.get());

        try(Producer<String, String> tracingProducer = telemetry.wrap(producer);) {
            int i = 0;
            while (produceMessages) {
                String message = "Message [" + i++ + "] at [" + System.currentTimeMillis() + "]";

                ProducerRecord<String, String> record = new ProducerRecord<>(kafkaTopic, message);
                tracingProducer.send(record, new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata metadata, Exception exception) {
                        if (exception == null) {
                            System.out.println("Producer record to topic" + metadata.topic() + ", partition " + metadata.partition() + ", offset "
                                    + metadata.offset());
                        } else {
                            System.err.println("XXXXX Error producing record: " + exception.getMessage());
                        }
                    }
                });
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(1));
                } catch (InterruptedException exc) {
                }
            }
        }finally {
                producer.close();
            }
    }
}
