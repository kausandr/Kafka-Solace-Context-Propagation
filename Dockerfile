FROM openjdk:8-jre-alpine

# Setting the working directory in the container
WORKDIR /app

COPY target/kafka-solace-context-propagation-1.0-SNAPSHOT.jar /app/kafka-solace-context-propagation-1.0-SNAPSHOT.jar

# Coping dependencies into a dedicated directory in the container
COPY target/dependency/* /app/lib/

# Adding the dependencies to the classpath
ENV CLASSPATH=/app/lib/*

#CMD ["java", "-jar", "/app/kafka-solace-context-propagation-1.0-SNAPSHOT.jar"]

# Specify the main class
CMD ["java", "-cp", "/app:/app/lib/*", "ConsumerApp"]