# PayU Quarkus Logging Configuration

Quarkus services require a different logging approach than Spring Boot because they use JBoss LogManager instead of Logback.

## Configuration

Add to `application.yml` or `application.properties`:

### application.yml

```yaml
# Quarkus Logging Configuration for PayU Standard
quarkus:
  log:
    console:
      # Enable JSON format for production
      json:
        ~: true
        # Pretty print for development (set to false in prod)
        pretty-print: false
      # Pattern for non-JSON fallback or development
      format: "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%X{correlation_id}] [%X{trace_id}] [%c{3.}] (%t) %s%e%n"
    level: INFO
    category:
      "id.payu":
        level: INFO
      "org.jboss":
        level: WARN

# PayU Logging Properties (for correlation ID handling)
payu:
  logging:
    service-name: ${quarkus.application.name}
    service-version: "1.0.0"
    environment: ${QUARKUS_PROFILE:dev}
    correlation:
      enabled: true
      header-name: "X-Correlation-Id"
```

### application.properties

```properties
# Console logging with JSON
quarkus.log.console.json=true
quarkus.log.console.json.pretty-print=false
quarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%X{correlation_id}] [%X{trace_id}] [%c{3.}] (%t) %s%e%n

# Log levels
quarkus.log.level=INFO
quarkus.log.category."id.payu".level=INFO
quarkus.log.category."org.jboss".level=WARN

# PayU configuration
payu.logging.service-name=${quarkus.application.name}
payu.logging.service-version=1.0.0
payu.logging.environment=${QUARKUS_PROFILE:dev}
payu.logging.correlation.enabled=true
payu.logging.correlation.header-name=X-Correlation-Id
```

## Correlation ID Filter

For Quarkus, create a JAX-RS filter:

```java
package id.payu.gateway.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

import java.io.IOException;
import java.util.UUID;

@Provider
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_MDC = "correlation_id";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String correlationId = requestContext.getHeaderString(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = generateCorrelationId();
        }
        MDC.put(CORRELATION_ID_MDC, correlationId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        String correlationId = MDC.get(CORRELATION_ID_MDC);
        if (correlationId != null) {
            responseContext.getHeaders().putSingle(CORRELATION_ID_HEADER, correlationId);
        }
        MDC.remove(CORRELATION_ID_MDC);
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
```

## OpenTelemetry Integration

Add dependencies to `pom.xml`:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-opentelemetry</artifactId>
</dependency>
```

Configure in `application.yml`:

```yaml
quarkus:
  opentelemetry:
    enabled: true
    tracer:
      enabled: true
      sampler:
        type: ratio
        ratio: 0.1
      exporter:
        otlp:
          endpoint: http://localhost:4317
```

## Log Format

### JSON Output (Production)

```json
{
  "timestamp": "2026-02-18T10:30:45.123Z",
  "level": "INFO",
  "message": "Request processed",
  "loggerName": "id.payu.gateway.GatewayResource",
  "threadName": "executor-thread-1",
  "correlation_id": "a1b2c3d4e5f6789",
  "trace_id": "abc123def456ghi789",
  "service": "gateway-service"
}
```

### Plain Text (Development)

```
2026-02-18 10:30:45,123 INFO [a1b2c3d4e5f6789] [abc123def456ghi789] [i.p.g.GatewayResource] (executor-thread-1) Request processed
```

## Loki Integration

Query examples:

```logql
# All logs from Quarkus service
{service="gateway-service"}

# By correlation ID
{correlation_id="a1b2c3d4e5f6789"}

# Errors only
{service="gateway-service"} |= "ERROR"
```

## Differences from Spring Boot

| Feature | Spring Boot | Quarkus |
|---------|-------------|---------|
| Config File | `logback-spring.xml` | `application.yml` |
| JSON Library | Logstash Encoder | Built-in JSON support |
| MDC Class | `org.slf4j.MDC` | `org.jboss.logging.MDC` |
| Filter Type | Servlet Filter | JAX-RS Filter |
| Tracing | OpenTelemetry starter | `quarkus-opentelemetry` |

## Services to Update

Apply this configuration to:
- gateway-service
- notification-service
- api-portal-service
