FROM openjdk:8-jre-alpine

COPY target/kafka-solace-context-propagation-1.0-SNAPSHOT.jar /app/kafka-solace-context-propagation-1.0-SNAPSHOT.jar

CMD ["java", "-jar", "/app/kafka-solace-context-propagation-1.0-SNAPSHOT.jar"]