import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class KafkaProducerApp {
    public static void main(String[] args) {
        String kafkaBootstrapServers = "tcp://kafka-ps:9092";
        String kafkaTopic = "kafkatest";

        Properties producerProp = new Properties();
        producerProp.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        producerProp.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProp.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        Producer<String, String> producer = new KafkaProducer<>(producerProp);

        try{
            for (int i = 0; i < 10; i++){
                String message = "Message" + i;

                ProducerRecord<String, String> record = new ProducerRecord<>(kafkaTopic, message);
                producer.send(record, new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata metadata, Exception exception) {
                        if(exception == null){
                            System.out.println("Producer record to topic" + metadata.topic() + ", partition " + metadata.partition() + ", offset "
                            + metadata.offset());
                        }else{
                            System.err.println("Error producing record: " + exception.getMessage());
                        }
                    }
                });
            }
        }finally {
            producer.close();
        }
    }
}
