# Webhook Handling Framework

## Overview

PayU Webhook Handling Framework provides a secure, reliable, and scalable solution for receiving and processing webhooks from external payment providers (BI-FAST, QRIS, external partners). The framework implements industry best practices for webhook security, idempotency, and asynchronous processing.

## Table of Contents

- [Architecture](#architecture)
- [Security Features](#security-features)
- [Quick Start](#quick-start)
- [Core Components](#core-components)
- [Configuration](#configuration)
- [Implementation Guide](#implementation-guide)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

---

## Architecture

### High-Level Flow

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  External       │────▶│  Your Service    │────▶│  202 Accepted   │
│  Provider       │     │  (Webhook Endpoint)│    │  Response       │
└─────────────────┘     └──────────────────┘     └─────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │  1. HMAC Verify  │
                       │  2. Idempotency  │
                       │  3. Quick ACK    │
                       └──────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │  Async Processor │
                       │  (Kafka/Thread)  │
                       └──────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │  Business Logic  │
                       │  Handler         │
                       └──────────────────┘
```

### Design Principles

1. **Quick ACK Pattern**: Return 202 Accepted immediately after validation to prevent provider timeouts
2. **Idempotency**: Prevent duplicate processing using Redis-based idempotency keys
3. **Security**: HMAC-SHA256 signature verification with constant-time comparison
4. **Reliability**: Exponential backoff retry mechanism for transient failures
5. **Observability**: Comprehensive logging and error tracking

---

## Security Features

### HMAC-SHA256 Signature Verification

All webhooks must include a signature header for authentication:

```
X-Webhook-Signature: sha256=<hex_encoded_hmac>
X-Webhook-Timestamp: <unix_timestamp_milliseconds>
X-Webhook-Id: <unique_webhook_identifier>
```

**Signature Construction:**
```
HMAC-SHA256(timestamp + "." + payload)
```

### Timestamp Tolerance

To prevent replay attacks, webhooks are rejected if the timestamp is:
- More than 5 minutes in the past (configurable)
- More than 5 seconds in the future (clock skew buffer)

### Constant-Time Comparison

Signature comparison uses `MessageDigest.isEqual()` to prevent timing attacks.

---

## Quick Start

### 1. Add Dependency

For Spring Boot services:

```xml
<dependency>
    <groupId>id.payu.shared</groupId>
    <artifactId>api-commons</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure Properties

```yaml
webhook:
  security:
    secret: \${WEBHOOK_SECRET}  # From environment variable or Vault
    tolerance-seconds: 300
  idempotency:
    ttl-hours: 24
  retry:
    max-attempts: 3
    initial-delay-ms: 1000
  kafka:
    topic: webhook-events
```

### 3. Create Webhook Handler

```java
@Component
public class MyWebhookHandler implements WebhookHandler {

    @Override
    public void processWebhook(String webhookId, String payload) {
        // Parse and process the webhook
        MyEvent event = parseEvent(payload);
        processEvent(event);
    }

    @Override
    public boolean validatePayload(String webhookId, String payload) {
        // Validate payload structure
        return payload.contains("required_field");
    }
}
```

### 4. Create REST Endpoint

```java
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookVerifier verifier;
    private final WebhookProcessor processor;
    private final MyWebhookHandler handler;

    @PostMapping
    public ResponseEntity<Void> receiveWebhook(
            @RequestHeader("X-Webhook-Signature") String signature,
            @RequestHeader("X-Webhook-Timestamp") long timestamp,
            @RequestHeader("X-Webhook-Id") String webhookId,
            @RequestBody String payload) {

        // 1. Verify signature
        if (!verifier.verify(payload, signature, timestamp)) {
            return ResponseEntity.status(401).build();
        }

        // 2. Check idempotency
        if (processor.isProcessed(webhookId)) {
            return ResponseEntity.status(409).build(); // Already processed
        }

        // 3. Acknowledge immediately
        processor.acknowledge(webhookId);

        // 4. Process asynchronously
        processor.processAsync(webhookId, payload, handler);

        return ResponseEntity.accepted().build(); // 202 Accepted
    }
}
```

---

## Core Components

### WebhookVerifier

Handles HMAC-SHA256 signature verification.

```java
@Component
@RequiredArgsConstructor
public class WebhookVerifier {

    /**
     * Verifies webhook signature with configured secret.
     */
    public boolean verify(String payload, String signature, long timestamp)

    /**
     * Verifies with specific secret (for multi-tenant scenarios).
     */
    public boolean verify(String payload, String signature, long timestamp, String secret)

    /**
     * Computes signature for testing/validation.
     */
    public String computeSignature(String payload, long timestamp, String secret)
}
```

### WebhookProcessor

Manages idempotency and asynchronous processing.

```java
@Component
@RequiredArgsConstructor
public class WebhookProcessor {

    /**
     * Check if webhook was already processed.
     */
    public boolean isProcessed(String webhookId)

    /**
     * Check if webhook is currently being processed.
     */
    public boolean isProcessing(String webhookId)

    /**
     * Acknowledge webhook (mark as received).
     */
    public void acknowledge(String webhookId)

    /**
     * Mark webhook as successfully processed.
     */
    public void markProcessed(String webhookId)

    /**
     * Mark webhook as failed.
     */
    public void markFailed(String webhookId, String errorMessage)

    /**
     * Process asynchronously via Kafka.
     */
    public void processAsync(String webhookId, String payload)

    /**
     * Process asynchronously with specific handler.
     */
    public void processAsync(String webhookId, String payload, WebhookHandler handler)

    /**
     * Clear idempotency record (use with caution).
     */
    public void clearIdempotency(String webhookId)
}
```

### WebhookHandler Interface

```java
public interface WebhookHandler {

    /**
     * Process the webhook payload.
     */
    void processWebhook(String webhookId, String payload);

    /**
     * Called on processing failure.
     */
    default void onError(String webhookId, Throwable error)

    /**
     * Called on processing success.
     */
    default void onSuccess(String webhookId, Object result)

    /**
     * Return supported event types (empty = all).
     */
    default String[] supportedEventTypes()

    /**
     * Validate payload before processing.
     */
    default boolean validatePayload(String webhookId, String payload)
}
```

### Exception Classes

```java
/**
 * Retryable processing error.
 */
public class WebhookProcessingException extends RuntimeException {
    private final String webhookId;
    private final boolean retryable;
}

/**
 * Non-retryable validation error.
 */
public class WebhookValidationException extends RuntimeException {
    private final String webhookId;
}
```

---

## Configuration

### Full Configuration Reference

```yaml
webhook:
  security:
    # Webhook secret for HMAC verification
    # SECURITY: Store in Vault or environment variable
    secret: \${WEBHOOK_SECRET:changeme}

    # Timestamp tolerance in seconds (replay attack prevention)
    # Range: 30-3600 seconds
    tolerance-seconds: 300

  idempotency:
    # How long to remember processed webhooks (hours)
    # Range: 1-168 hours (1 week)
    ttl-hours: 24

  retry:
    # Maximum retry attempts for failed webhooks
    # Range: 0-10
    max-attempts: 3

    # Initial delay before first retry (milliseconds)
    # Range: 100-60000
    initial-delay-ms: 1000

  kafka:
    # Kafka topic for async webhook events
    topic: webhook-events
```

### Multi-Tenant Configuration

For services handling webhooks from multiple partners:

```java
@Component
public class PartnerWebhookVerifier {

    private final WebhookVerifier verifier;
    private final PartnerRepository partnerRepository;

    public boolean verifyForPartner(String partnerId, String payload,
                                    String signature, long timestamp) {
        Partner partner = partnerRepository.findById(partnerId)
            .orElseThrow(() -> new PartnerNotFoundException(partnerId));

        return verifier.verify(payload, signature, timestamp, partner.getWebhookSecret());
    }
}
```

---

## Implementation Guide

### Example: Payment Webhook Handler

See \`/backend/partner-service/src/main/java/id/payu/partner/webhook/PaymentWebhookHandler.java\` for a complete implementation.

Key implementation points:

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentWebhookHandler implements WebhookHandler {

    private final ObjectMapper objectMapper;
    private final PaymentNotificationService notificationService;

    @Override
    public String[] supportedEventTypes() {
        return new String[]{
            "payment.completed",
            "payment.failed",
            "payment.pending",
            "payment.refunded"
        };
    }

    @Override
    public boolean validatePayload(String webhookId, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);

            // Validate required fields
            if (!root.has("event") || !root.has("data")) {
                return false;
            }

            JsonNode data = root.get("data");
            if (!data.has("transactionId") || !data.has("amount")) {
                return false;
            }

            // Validate event type
            String eventType = root.get("event").asText();
            return isSupportedEventType(eventType);

        } catch (Exception e) {
            log.warn("Failed to parse webhook payload: id={}", webhookId, e);
            return false;
        }
    }

    @Override
    public void processWebhook(String webhookId, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.get("event").asText();

            switch (eventType) {
                case "payment.completed":
                    handlePaymentCompleted(parseEvent(root));
                    break;
                case "payment.failed":
                    handlePaymentFailed(parseEvent(root));
                    break;
                // ... other cases
            }
        } catch (Exception e) {
            log.error("Failed to process payment webhook: id={}", webhookId, e);
            throw new WebhookProcessingException(webhookId, "Processing failed", e);
        }
    }

    @Override
    public void onSuccess(String webhookId, Object result) {
        log.info("Payment webhook processed successfully: id={}", webhookId);
    }

    @Override
    public void onError(String webhookId, Throwable error) {
        log.error("Payment webhook processing failed: id={}", webhookId, error);
        // Send alert to operations team
    }
}
```

### Testing Webhook Handlers

```java
@SpringBootTest
class PaymentWebhookHandlerTest {

    @Autowired
    private PaymentWebhookHandler handler;

    @Test
    void shouldProcessPaymentCompleted() {
        String payload = """
            {
                "event": "payment.completed",
                "data": {
                    "transactionId": "TXN-123",
                    "amount": 100000.00,
                    "currency": "IDR"
                }
            }
            """;

        assertTrue(handler.validatePayload("webhook-123", payload));

        // Should not throw
        handler.processWebhook("webhook-123", payload);
    }

    @Test
    void shouldRejectInvalidPayload() {
        String payload = """
            {
                "event": "payment.completed"
                // Missing "data" field
            }
            """;

        assertFalse(handler.validatePayload("webhook-123", payload));
    }
}
```

---

## Best Practices

### 1. Always Return 202 Accepted Quickly

```java
// GOOD: Quick acknowledgment
@PostMapping
public ResponseEntity<Void> receiveWebhook(...) {
    if (!verifier.verify(...)) {
        return ResponseEntity.status(401).build();
    }
    processor.acknowledge(webhookId);
    processor.processAsync(webhookId, payload, handler);
    return ResponseEntity.accepted().build(); // Return immediately
}

// BAD: Synchronous processing
@PostMapping
public ResponseEntity<Void> receiveWebhook(...) {
    handler.processWebhook(webhookId, payload); // Don't do this!
    return ResponseEntity.ok().build();
}
```

### 2. Implement Proper Idempotency

```java
@Override
public void processWebhook(String webhookId, String payload) {
    // Check business-level idempotency
    if (transactionRepository.existsByWebhookId(webhookId)) {
        log.info("Duplicate webhook detected: id={}", webhookId);
        return; // Silently ignore
    }

    // Process the webhook
    processTransaction(payload);
}
```

### 3. Distinguish Retryable vs Non-Retryable Errors

```java
@Override
public void processWebhook(String webhookId, String payload) {
    try {
        PaymentEvent event = parseEvent(payload);

        if (event.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            // Non-retryable: Invalid amount
            throw new WebhookValidationException(webhookId, "Invalid amount");
        }

        // Call external service
        externalService.process(event);

    } catch (ExternalServiceUnavailableException e) {
        // Retryable: External service is down
        throw new WebhookProcessingException(webhookId, "Service unavailable", e);
    }
}
```

### 4. Secure Your Webhook Secret

```yaml
# application.yml - NEVER commit secrets
webhook:
  security:
    secret: \${WEBHOOK_SECRET}  # From environment

# docker-compose.yml
services:
  app:
    environment:
      - WEBHOOK_SECRET=\${WEBHOOK_SECRET}

# OpenShift Secret
# oc create secret generic webhook-secret --from-literal=secret=your-secret
```

### 5. Monitor Webhook Metrics

```java
@Component
public class WebhookMetrics {

    private final MeterRegistry meterRegistry;

    public void recordSuccess(String eventType) {
        meterRegistry.counter("webhook.processed",
            "status", "success",
            "event_type", eventType).increment();
    }

    public void recordFailure(String eventType, String reason) {
        meterRegistry.counter("webhook.processed",
            "status", "failure",
            "event_type", eventType,
            "reason", reason).increment();
    }
}
```

---

## Troubleshooting

### Common Issues

#### 1. Signature Verification Fails

**Symptoms:** 401 Unauthorized responses

**Possible Causes:**
- Incorrect webhook secret
- Clock skew between systems
- Payload tampering

**Solutions:**
```java
// Enable debug logging
logging.level.id.payu.api.common.webhook: DEBUG

// Verify signature manually for testing
String computed = verifier.computeSignature(payload, timestamp, secret);
System.out.println("Expected: " + signature);
System.out.println("Computed: " + computed);
```

#### 2. Duplicate Processing

**Symptoms:** Multiple transactions for single webhook

**Solutions:**
- Ensure Redis is properly configured
- Check idempotency TTL settings
- Implement business-level idempotency checks

#### 3. Webhook Timeouts

**Symptoms:** Provider reports timeouts

**Solutions:**
- Verify 202 response is returned immediately
- Check async processing is not blocking
- Review thread pool configuration

```yaml
spring:
  task:
    execution:
      pool:
        core-size: 10
        max-size: 50
        queue-capacity: 100
```

#### 4. Retry Exhaustion

**Symptoms:** Webhooks marked as failed after retries

**Solutions:**
- Check error logs for root cause
- Increase retry attempts if transient errors
- Implement dead letter queue (DLQ) for manual review

### Debugging Checklist

1. [ ] Verify webhook secret is correctly configured
2. [ ] Check system clocks are synchronized (NTP)
3. [ ] Confirm Redis connectivity for idempotency
4. [ ] Review handler logs for processing errors
5. [ ] Check Kafka connectivity (if using async)
6. [ ] Verify payload format matches expected schema

---

## API Reference

### Headers

| Header | Required | Description |
|--------|----------|-------------|
| \`X-Webhook-Signature\` | Yes | HMAC-SHA256 signature |
| \`X-Webhook-Timestamp\` | Yes | Unix timestamp (milliseconds) |
| \`X-Webhook-Id\` | Yes | Unique webhook identifier |
| \`Content-Type\` | Yes | \`application/json\` |

### Response Codes

| Code | Meaning | Action |
|------|---------|--------|
| 202 | Accepted | Webhook queued for processing |
| 401 | Unauthorized | Invalid signature |
| 409 | Conflict | Duplicate webhook (already processed) |
| 422 | Unprocessable | Invalid payload format |
| 500 | Server Error | Retry will be attempted |

---

## Additional Resources

- [OWASP Webhook Security](https://cheatsheetseries.owasp.org/cheatsheets/Webhook_Security_Cheat_Sheet.html)
- [Stripe Webhook Best Practices](https://stripe.com/docs/webhooks/best-practices)
- [PayU Security Guidelines](../security/SECURITY.md)

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-01-30 | Initial release with HMAC + Idempotency |

---

**Maintained by:** PayU Platform Engineering Team
**Last Updated:** January 2026
