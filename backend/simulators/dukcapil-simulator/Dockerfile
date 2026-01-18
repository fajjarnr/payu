####
# Build stage
####
FROM registry.access.redhat.com/ubi9/openjdk-21:1.20 AS build

WORKDIR /build

# Copy Maven files
COPY --chown=185 pom.xml ./
COPY --chown=185 .mvn .mvn
COPY --chown=185 mvnw ./

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source
COPY --chown=185 src src

# Build
RUN ./mvnw package -DskipTests -Dquarkus.package.jar.type=uber-jar

####
# Runtime stage
####
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.20

ENV LANGUAGE='en_US:en'
ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/app.jar"

WORKDIR /deployments

# Copy built artifact
COPY --from=build --chown=185 /build/target/*-runner.jar /deployments/app.jar

EXPOSE 8091

USER 185

ENTRYPOINT [ "java", "-jar", "/deployments/app.jar" ]
