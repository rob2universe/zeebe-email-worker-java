FROM adoptopenjdk/openjdk11:jre-11.0.14.1_1-alpine
RUN addgroup -S camunda && adduser -S camunda -G camunda
WORKDIR /app
COPY ./target/zeebe-email-worker-springboot-1.0-SNAPSHOT.jar .
RUN chown -R camunda:camunda /app
USER camunda:camunda
RUN chmod 755 /app
ENTRYPOINT ["java","-jar","zeebe-email-worker-springboot-1.0-SNAPSHOT.jar"]


