#!/bin/bash
set -e

# ============================================
# PayU — Service Template Scaffolder (DEVSECOPS-016)
# Scaffolds a secure, hexagonal Spring Boot service
# following PayU platform conventions:
#   - backend parent POM (payu-backend-parent)
#   - Hexagonal layout: adapter/application/domain + interfaces.dto
#   - Shared starters: security-starter, logging-starter, archunit-starter
#   - UBI9 non-root Containerfile (UID 1001, read-only /deployments)
#   - ArchUnit layered-architecture test
# ============================================

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
    fi
}
print_info() { echo -e "${BLUE}ℹ${NC} $1"; }
print_warning() { echo -e "${YELLOW}⚠${NC} $1"; }

usage() {
    echo "Usage: $0 --name <service-name> [--description <desc>] [--module <hexagonal|flat>]"
    echo ""
    echo "Examples:"
    echo "  $0 --name my-service --description 'My new service'"
    echo "  $0 --name my-service --module flat   # flat-package layout (refactor later)"
    exit 1
}

# --- Parse args ---
SERVICE_NAME=""
DESCRIPTION=""
MODULE="hexagonal"

while [ $# -gt 0 ]; do
    case "$1" in
        --name) SERVICE_NAME="$2"; shift 2 ;;
        --description) DESCRIPTION="$2"; shift 2 ;;
        --module) MODULE="$2"; shift 2 ;;
        *) echo -e "${RED}Unknown argument:${NC} $1"; usage ;;
    esac
done

if [ -z "$SERVICE_NAME" ]; then
    usage
fi

# Validate service name (kebab-case alphanumeric)
if ! [[ "$SERVICE_NAME" =~ ^[a-z0-9]+(-[a-z0-9]+)*$ ]]; then
    echo -e "${RED}Error:${NC} Service name must be kebab-case (e.g. 'my-service')"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SERVICE_DIR="$ROOT_DIR/backend/$SERVICE_NAME"
PKG="id.payu.${SERVICE_NAME//-/}"
PKG_PATH="${PKG//.//}"

if [ -d "$SERVICE_DIR" ]; then
    echo -e "${RED}Error:${NC} Service '$SERVICE_NAME' already exists at $SERVICE_DIR"
    exit 1
fi

echo "=========================================="
echo "Scaffolding: $SERVICE_NAME (module=$MODULE)"
echo "Target: $SERVICE_DIR"
echo "=========================================="
echo ""

mkdir -p "$SERVICE_DIR/src/main/java/$PKG_PATH" \
         "$SERVICE_DIR/src/main/resources" \
         "$SERVICE_DIR/src/test/java/$PKG_PATH"

# --- Containerfile (UBI9, non-root, read-only) ---
cat > "$SERVICE_DIR/Containerfile" <<EOF
FROM registry.access.redhat.com/ubi9/openjdk-25-runtime:1.24

ARG APP_VERSION=1.0.0
LABEL org.opencontainers.image.title="payu-$SERVICE_NAME"
LABEL org.opencontainers.image.version="\${APP_VERSION}"
LABEL org.opencontainers.image.vendor="PayU Indonesia"

USER root
RUN microdnf install -y curl && \\
    microdnf clean all && \\
    useradd -u 1001 payu && \\
    mkdir -p /deployments && \\
    chown -R 1001:0 /deployments

USER 1001
WORKDIR /deployments
COPY --chown=1001 target/app.jar /deployments/app.jar
EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom"
HEALTHCHECK --interval=30s --timeout=3s --start-period=90s --retries=3 \\
    CMD curl -sf http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["sh", "-c", "java \$JAVA_OPTS -jar /deployments/app.jar"]
EOF

# --- pom.xml (PayU parent) ---
cat > "$SERVICE_DIR/pom.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>id.payu</groupId>
        <artifactId>payu-backend-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <groupId>id.payu</groupId>
    <artifactId>$SERVICE_NAME</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>$SERVICE_NAME</name>
    <description>$DESCRIPTION</description>

    <properties>
        <maven.compiler.source>25</maven.compiler.source>
        <maven.compiler.target>25</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>id.payu</groupId>
            <artifactId>logging-starter</artifactId>
            <version>\${project.version}</version>
        </dependency>
        <dependency>
            <groupId>id.payu</groupId>
            <artifactId>security-starter</artifactId>
            <version>\${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <version>\${archunit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-webmvc-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <mainClass>$PKG.${SERVICE_NAME^}ServiceApplication</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOF

# --- Main application class ---
CLASS_NAME="$(echo "$SERVICE_NAME" | sed -E 's/(^|-)([a-z])/\U\2/g')ServiceApplication"
cat > "$SERVICE_DIR/src/main/java/$PKG_PATH/${CLASS_NAME}.java" <<EOF
package $PKG;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ${CLASS_NAME} {

    public static void main(String[] args) {
        SpringApplication.run(${CLASS_NAME}.class, args);
    }
}
EOF

# --- Hexagonal skeleton ---
if [ "$MODULE" = "hexagonal" ]; then
    for p in adapter/web interfaces/dto application/service domain/model domain/port/in domain/port/out config; do
        mkdir -p "$SERVICE_DIR/src/main/java/$PKG_PATH/$p"
    done

    # Health endpoint (public probe pattern used across PayU)
    mkdir -p "$SERVICE_DIR/src/main/java/$PKG_PATH/adapter/web"
    cat > "$SERVICE_DIR/src/main/java/$PKG_PATH/adapter/web/HealthController.java" <<EOF
package $PKG.adapter.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/public/health")
public class HealthController {

    @GetMapping
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", "$SERVICE_NAME");
    }
}
EOF

    # ArchUnit test
    cat > "$SERVICE_DIR/src/test/java/$PKG_PATH/ArchitectureTest.java" <<EOF
package $PKG;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "$PKG")
public class ArchitectureTest {

    @ArchTest
    static final ArchRule hexagonal_architecture = layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage("$PKG..")
            .layer("Adapter.Web").definedBy("..adapter.web..")
            .layer("Application").definedBy("..application..")
            .layer("Domain").definedBy("..domain..")
            .layer("Config").definedBy("..config..")
            .layer("Dto").definedBy("..interfaces.dto..")

            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Adapter.Web", "Application", "Dto", "Config")
            .whereLayer("Dto").mayOnlyBeAccessedByLayers("Adapter.Web", "Application", "Config")
            .withOptionalLayers(true);
}
EOF

    # Health controller test — uses @SpringBootTest + @AutoConfigureMockMvc,
    # the proven Boot 4 pattern across PayU services (WebMvcTest moved to a
    # separate spring-boot-webmvc-test module in Boot 4, per Context7).
    cat > "$SERVICE_DIR/src/test/java/$PKG_PATH/HealthControllerTest.java" <<EOF
package $PKG;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthReturnsUp() throws Exception {
        mockMvc.perform(get("/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
EOF
fi

# --- application.yml ---
cat > "$SERVICE_DIR/src/main/resources/application.yml" <<EOF
spring:
  application:
    name: $SERVICE_NAME
  jpa:
    open-in-view: false

server:
  port: 8080
  shutdown: graceful

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      probes:
        enabled: true
EOF

# --- test application.yml (H2, no Flyway, dummy OIDC) ---
mkdir -p "$SERVICE_DIR/src/test/resources"
cat > "$SERVICE_DIR/src/test/resources/application.yml" <<EOF
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
  flyway:
    enabled: false
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:18080/realms/payu

logging:
  level:
    root: WARN
EOF

# --- Register module in backend parent ---
if grep -q "<module>$SERVICE_NAME</module>" "$ROOT_DIR/backend/pom.xml"; then
    print_warning "Module already registered in backend/pom.xml"
else
    # Insert before </modules>
    sed -i "/<\/modules>/i\\        <module>$SERVICE_NAME</module>" "$ROOT_DIR/backend/pom.xml"
    print_status 0 "Registered module in backend/pom.xml"
fi

echo ""
echo "=========================================="
echo -e "${GREEN}✅ $SERVICE_NAME scaffolded${NC}"
echo "=========================================="
echo ""
print_info "Verify it builds:"
echo "  mvn -f backend/pom.xml package -pl $SERVICE_NAME -am -DskipTests"
echo ""
print_info "Then add your domain ports, adapters, and Flyway migrations (V1__init.sql)."
echo ""
print_info "Next steps:"
echo "  - Register in infrastructure/local/podman/podman-compose.yml"
echo "  - Add route to backend/gateway-service/src/main/resources/application.yaml"
echo "  - Add E2E journey under tests/e2e/"
