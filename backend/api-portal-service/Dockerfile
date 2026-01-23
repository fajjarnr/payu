FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.20

WORKDIR /app

COPY --chown=185 target/*.jar app.jar

RUN chmod 664 app.jar

EXPOSE 8099

USER 185

ENTRYPOINT ["java", "-jar", "app.jar"]
