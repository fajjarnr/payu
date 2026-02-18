# PayU Logging Starter

Standardized logging configuration for PayU microservices with JSON formatting, MDC support, and OpenTelemetry integration.

## Features

- **JSON Logging**: Logstash-compatible JSON format for LokiStack integration
- **MDC Support**: Automatic correlation ID propagation via HTTP headers
- **OpenTelemetry**: Trace and span ID extraction for distributed tracing
- **Service Metadata**: Service name, version, and environment in every log entry
- **Profile-based**: Plain text for dev, JSON for staging/production
- **Async Logging**: Non-blocking log appender for production performance

## Installation

Add dependency to your service's `pom.xml`:

```xml
<dependency>
    <groupId>id.payu</groupId>
    <artifactId>logging-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Configuration

### 1. Add logback configuration

Create or update `src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="logback-payu-base.xml"/>
</configuration>
```

### 2. Configure application.yml

```yaml
payu:
  logging:
    service-name: ${spring.application.name}
    service-version: "1.0.0"
    environment: ${SPRING_PROFILES_ACTIVE:dev}
    correlation:
      enabled: true
      header-name: "X-Correlation-Id"
      mdc-key: "correlation_id"
    tracing:
      enabled: true

spring:
  application:
    name: "transaction-service"
```

## Log Format

### JSON Output (Production)

```json
{
  "@timestamp": "2026-02-18T10:30:45.123Z",
  "message": "Transaction processed successfully",
  "logger": "id.payu.transaction.service.TransactionService",
  "thread": "http-nio-8080-exec-1",
  "level": "INFO",
  "correlation_id": "a1b2c3d4e5f6789",
  "trace_id": "abc123def456ghi789",
  "span_id": "jkl012mno345pqr678",
  "service": "transaction-service",
  "service_version": "1.0.0",
  "environment": "prod"
}
```

### Plain Text (Development)

```
2026-02-18 10:30:45.123 [http-nio-8080-exec-1] INFO [a1b2c3d4e5f6789] [abc123def456ghi789] i.p.t.s.TransactionService - Transaction processed successfully
```

## Usage

### Automatic Correlation ID

The starter automatically:
1. Reads `X-Correlation-Id` header from incoming requests
2. Generates a new UUID if header is missing
3. Sets it in MDC for all logs in the request
4. Returns the correlation ID in response headers

### Manual MDC Manipulation

```java
import id.payu.logging.util.MdcUtil;

@Service
public class TransactionService {

    @Autowired
    private MdcUtil mdcUtil;

    public void processTransaction(Transaction tx) {
        // Execute with new correlation ID
        mdcUtil.withCorrelationId(() -> {
            log.info("Processing transaction: {}", tx.getId());
            // All logs here will have the same correlation_id
        });
    }
}
```

### With OpenTelemetry

When OpenTelemetry is configured, trace and span IDs are automatically added to logs:

```java
@WithSpan("process-payment")
public PaymentResult processPayment(PaymentRequest request) {
    log.info("Processing payment for amount: {}", request.getAmount());
    // Logs will include trace_id and span_id automatically
}
```

## Loki Integration

The JSON format is compatible with Grafana Loki:

```yaml
# Loki query example
{service="transaction-service"} |= "error" | json
```

Query by correlation ID:
```
{correlation_id="a1b2c3d4e5f6789"}
```

Query by trace:
```
{trace_id="abc123def456ghi789"}
```

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `payu.logging.enabled` | `true` | Enable auto-configuration |
| `payu.logging.service-name` | - | Service identifier in logs |
| `payu.logging.service-version` | `1.0.0` | Service version |
| `payu.logging.environment` | `dev` | Environment name |
| `payu.logging.correlation.enabled` | `true` | Enable correlation ID filter |
| `payu.logging.correlation.header-name` | `X-Correlation-Id` | HTTP header name |
| `payu.logging.correlation.mdc-key` | `correlation_id` | MDC key name |
| `payu.logging.tracing.enabled` | `true` | Enable trace ID filter |

## Migration Guide

### From Plain Text Logging

1. Add `logging-starter` dependency
2. Create `logback-spring.xml` including `logback-payu-base.xml`
3. Remove old logging configuration
4. Update Loki queries to use JSON field names

### From Custom JSON Logging

1. Remove custom `LogstashEncoder` configuration
2. Add `logging-starter` dependency
3. Use standard `logback-payu-base.xml`
4. Update any custom MDC keys to standard names
