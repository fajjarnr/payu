FROM registry.access.redhat.com/ubi9/openjdk-21:1.18 AS builder

USER root
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN microdnf install -y maven && \
    mvn package -DskipTests && \
    microdnf clean all

FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.18

LABEL maintainer="backend-team@payu.id"
LABEL description="PayU Notification Service - Push, SMS, Email"

USER 185
WORKDIR /app

COPY --from=builder /app/target/quarkus-app/lib/ ./lib/
COPY --from=builder /app/target/quarkus-app/*.jar ./
COPY --from=builder /app/target/quarkus-app/app/ ./app/
COPY --from=builder /app/target/quarkus-app/quarkus/ ./quarkus/

EXPOSE 8086

ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8086/q/health || exit 1

ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
