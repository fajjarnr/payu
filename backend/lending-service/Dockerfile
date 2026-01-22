FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.21

LABEL name="lending-service" \
      maintainer="PayU Team" \
      version="1.0.0"

WORKDIR /app

COPY target/lending-service-*.jar app.jar

RUN chmod +x app.jar

EXPOSE 8087

ENTRYPOINT ["java", "-jar", "app.jar"]
