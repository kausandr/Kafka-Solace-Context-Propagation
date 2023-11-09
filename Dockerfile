FROM openjdk:8-jre-alpine

COPY target/kafka-solace-context-propagation-1.0-SNAPSHOT.jar /app/kafka-solace-context-propagation-1.0-SNAPSHOT.jar

# # Coping dependencies into a dedicated directory in the container
# COPY target/dependency/* /app/lib/
#
# # Adding the dependencies to the classpath
# ENV CLASSPATH=/app/lib/*
#
# # Specify the main class
# CMD ["java", "-cp", "/app:/app/lib/*", "ConsumerApp"]

ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar .
ENV JAVA_TOOL_OPTIONS "-javaagent:./opentelemetry-javaagent.jar"

CMD ["java", "-cp", "/app/kafka-solace-context-propagation-1.0-SNAPSHOT.jar", "ConsumerApp"]
