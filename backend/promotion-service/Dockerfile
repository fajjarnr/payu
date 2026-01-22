# Stage 1: Build with Maven
FROM registry.access.redhat.com/ubi9/openjdk-21:1.20 AS builder

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests -T 1C

# Stage 2: Run on UBI9 minimal
FROM registry.access.redhat.com/ubi9/ubi-minimal:9.5

ARG JAVA_PACKAGE=java-21-openjdk-headless
ARG RUN_JAVA_VERSION=21

USER root

RUN microdnf install -y ${JAVA_PACKAGE}-${RUN_JAVA_VERSION} \
    && microdnf clean all \
    && mkdir -p /deployments

COPY --from=builder /build/target/quarkus-app/lib /deployments/lib
COPY --from=builder /build/target/quarkus-app/quarkus-run.jar /deployments/app.jar

WORKDIR /deployments

RUN chmod -R 755 /deployments

USER 185

ENV JAVA_OPTS_APPEND="-XX:+UseG1GC -XX:+UseStringDeduplication -XX:MaxRAMPercentage=75.0"

EXPOSE 8088 8443

ENTRYPOINT [ "/usr/bin/java" ]
CMD [ "$JAVA_OPTS_APPEND", "-jar", "app.jar" ]
