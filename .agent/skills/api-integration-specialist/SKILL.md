---
name: API Integration Specialist
description: Expert in integrating third-party APIs with proper authentication, error handling, rate limiting, and retry logic. Use when integrating REST APIs, GraphQL endpoints, webhooks, or external services. Specializes in OAuth flows, API key management, request/response transformation, and building robust API clients.
---

# API Integration Specialist

Expert guidance for integrating external APIs into applications with production-ready patterns, security best practices, and comprehensive error handling.

## When to Use This Skill

Use this skill when:
- Integrating third-party APIs (Stripe, Twilio, SendGrid, etc.)
- Building API client libraries or wrappers
- Implementing OAuth 2.0, API keys, or JWT authentication
- Setting up webhooks and event-driven integrations
- Handling rate limits, retries, and circuit breakers
- Transforming API responses for application use
- Debugging API integration issues

## Core Integration Principles

### 1. Authentication & Security

**API Key Management:**
```javascript
// Store keys in environment variables, never in code
const apiClient = new APIClient({
  apiKey: process.env.SERVICE_API_KEY,
  baseURL: process.env.SERVICE_BASE_URL
});
```

**OAuth 2.0 Flow:**
```javascript
// Authorization Code Flow
const oauth = new OAuth2Client({
  clientId: process.env.CLIENT_ID,
  clientSecret: process.env.CLIENT_SECRET,
  redirectUri: process.env.REDIRECT_URI,
  scopes: ['read:users', 'write:data']
});

// Get authorization URL
const authUrl = oauth.getAuthorizationUrl();

// Exchange code for tokens
const tokens = await oauth.exchangeCode(code);
```

### 2. Request/Response Handling

**Standardized Request Structure:**
```javascript
async function makeRequest(endpoint, options = {}) {
  const defaultHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${apiKey}`,
    'User-Agent': 'MyApp/1.0.0'
  };

  const response = await fetch(`${baseURL}${endpoint}`, {
    ...options,
    headers: { ...defaultHeaders, ...options.headers }
  });

  if (!response.ok) {
    throw new APIError(response.status, await response.json());
  }

  return response.json();
}
```

**Response Transformation:**
```javascript
class APIClient {
  async getUser(userId) {
    const raw = await this.request(`/users/${userId}`);

    // Transform external API format to internal model
    return {
      id: raw.user_id,
      email: raw.email_address,
      name: `${raw.first_name} ${raw.last_name}`,
      createdAt: new Date(raw.created_timestamp)
    };
  }
}
```

### 3. Error Handling

**Structured Error Types:**
```javascript
class APIError extends Error {
  constructor(status, body) {
    super(`API Error: ${status}`);
    this.status = status;
    this.body = body;
    this.isAPIError = true;
  }

  isRateLimited() {
    return this.status === 429;
  }

  isUnauthorized() {
    return this.status === 401;
  }

  isServerError() {
    return this.status >= 500;
  }
}
```

**Retry Logic with Exponential Backoff:**
```javascript
async function retryWithBackoff(fn, maxRetries = 3) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fn();
    } catch (error) {
      if (!error.isAPIError || !error.isServerError()) {
        throw error; // Don't retry client errors
      }

      if (i === maxRetries - 1) throw error;

      const delay = Math.pow(2, i) * 1000; // 1s, 2s, 4s
      await sleep(delay);
    }
  }
}
```

### 4. Rate Limiting

**Client-Side Rate Limiter:**
```javascript
class RateLimiter {
  constructor(maxRequests, windowMs) {
    this.maxRequests = maxRequests;
    this.windowMs = windowMs;
    this.requests = [];
  }

  async acquire() {
    const now = Date.now();
    this.requests = this.requests.filter(t => now - t < this.windowMs);

    if (this.requests.length >= this.maxRequests) {
      const oldestRequest = this.requests[0];
      const waitTime = this.windowMs - (now - oldestRequest);
      await sleep(waitTime);
      return this.acquire();
    }

    this.requests.push(now);
  }
}

const limiter = new RateLimiter(100, 60000); // 100 requests per minute

async function rateLimitedRequest(endpoint, options) {
  await limiter.acquire();
  return makeRequest(endpoint, options);
}
```

### 5. Webhook Handling (Inbound)

Webhooks are critical for PayU's partner integrations (BI-FAST, QRIS, external payment gateways). Follow these patterns for secure, reliable webhook handling.

#### Webhook Verification

**Spring Boot Implementation with HMAC Signature:**
```java
@RestController
@RequestMapping("/api/v1/webhooks")
@Slf4j
@RequiredArgsConstructor
public class WebhookController {
    
    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;
    
    @PostMapping("/bifast")
    public ResponseEntity<Void> handleBifastWebhook(
            @RequestBody String payload,
            @RequestHeader("X-BIFAST-Signature") String signature,
            @RequestHeader("X-BIFAST-Timestamp") String timestamp,
            @RequestHeader("X-BIFAST-Idempotency-Key") String idempotencyKey) {
        
        // 1. Verify timestamp (prevent replay attacks)
        if (!isTimestampValid(timestamp)) {
            log.warn("Webhook timestamp too old: {}", timestamp);
            return ResponseEntity.status(401).build();
        }
        
        // 2. Verify signature
        String secret = webhookService.getWebhookSecret("BIFAST");
        if (!verifySignature(payload, signature, secret, timestamp)) {
            log.warn("Invalid webhook signature from BI-FAST");
            return ResponseEntity.status(401).build();
        }
        
        // 3. Check idempotency
        if (webhookService.isProcessed(idempotencyKey)) {
            log.info("Duplicate webhook received: {}", idempotencyKey);
            return ResponseEntity.ok().build(); // Already processed, return success
        }
        
        // 4. Process asynchronously
        webhookService.processBifastWebhookAsync(payload, idempotencyKey);
        
        return ResponseEntity.accepted().build(); // 202 Accepted
    }
    
    private boolean verifySignature(String payload, String signature, String secret, String timestamp) {
        try {
            String data = timestamp + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);
            
            return MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }
    
    private boolean isTimestampValid(String timestamp) {
        try {
            Instant webhookTime = Instant.ofEpochSecond(Long.parseLong(timestamp));
            Instant now = Instant.now();
            // Allow 5 minutes tolerance
            return Duration.between(webhookTime, now).abs().toMinutes() <= 5;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
```

**Webhook Service with Retry Logic:**
```java
@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookService {
    
    private final WebhookEventRepository repository;
    private final KafkaTemplate<String, WebhookEvent> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    
    @Async("webhookExecutor")
    public void processBifastWebhookAsync(String payload, String idempotencyKey) {
        try {
            BifastWebhookEvent event = parsePayload(payload);
            
            // Store event for audit trail
            WebhookEventEntity entity = WebhookEventEntity.builder()
                .idempotencyKey(idempotencyKey)
                .source("BIFAST")
                .eventType(event.getEventType())
                .payload(payload)
                .status(WebhookStatus.PROCESSING)
                .receivedAt(Instant.now())
                .build();
            
            repository.save(entity);
            
            // Publish to Kafka for async processing
            kafkaTemplate.send("bifast.webhooks", event.getTransactionId(), event);
            
            // Mark as processed
            entity.setStatus(WebhookStatus.PROCESSED);
            entity.setProcessedAt(Instant.now());
            repository.save(entity);
            
            // Cache idempotency key
            redisTemplate.opsForValue().set(
                "webhook:" + idempotencyKey,
                "processed",
                Duration.ofHours(24)
            );
            
        } catch (Exception e) {
            log.error("Failed to process BI-FAST webhook: {}", idempotencyKey, e);
            // Store for retry
            storeForRetry(idempotencyKey, payload, e);
        }
    }
    
    @Scheduled(fixedDelay = 60000) // Every minute
    public void retryFailedWebhooks() {
        List<WebhookEventEntity> failed = repository.findByStatusAndRetryCountLessThan(
            WebhookStatus.FAILED, 5
        );
        
        for (WebhookEventEntity event : failed) {
            try {
                processBifastWebhookAsync(event.getPayload(), event.getIdempotencyKey());
                event.setRetryCount(event.getRetryCount() + 1);
                repository.save(event);
            } catch (Exception e) {
                log.error("Retry failed for webhook: {}", event.getIdempotencyKey(), e);
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 5) {
                    event.setStatus(WebhookStatus.PERMANENTLY_FAILED);
                    alertOpsTeam(event);
                }
                repository.save(event);
            }
        }
    }
    
    public boolean isProcessed(String idempotencyKey) {
        return Boolean.TRUE.equals(
            redisTemplate.hasKey("webhook:" + idempotencyKey)
        );
    }
    
    public String getWebhookSecret(String source) {
        // Fetch from Vault or secure config
        return System.getenv(source + "_WEBHOOK_SECRET");
    }
}
```

**QRIS Webhook Handler:**
```java
@RestController
@RequestMapping("/api/v1/webhooks/qris")
@Slf4j
@RequiredArgsConstructor
public class QrisWebhookController {
    
    private final QrisPaymentService qrisService;
    private final WebhookSignatureValidator signatureValidator;
    
    @PostMapping
    public ResponseEntity<QrisWebhookResponse> handleQrisPayment(
            @RequestBody QrisCallbackRequest request,
            @RequestHeader("X-QRIS-Signature") String signature) {
        
        // Validate signature
        String secret = System.getenv("QRIS_WEBHOOK_SECRET");
        if (!signatureValidator.validate(request, signature, secret)) {
            return ResponseEntity.status(401)
                .body(new QrisWebhookResponse("99", "Invalid signature"));
        }
        
        try {
            // Process payment callback
            QrisPaymentResult result = qrisService.processCallback(request);
            
            // Return QRIS standard response
            return ResponseEntity.ok(new QrisWebhookResponse(
                "00", 
                "Success",
                result.getTransactionId()
            ));
            
        } catch (PaymentNotFoundException e) {
            log.error("QRIS payment not found: {}", request.getReferenceNo());
            return ResponseEntity.ok(new QrisWebhookResponse(
                "01", 
                "Transaction not found"
            ));
        } catch (Exception e) {
            log.error("Failed to process QRIS webhook", e);
            return ResponseEntity.ok(new QrisWebhookResponse(
                "99", 
                "System error"
            ));
        }
    }
}
```

**Webhook Event Entity:**
```java
@Entity
@Table(name = "webhook_events", indexes = {
    @Index(name = "idx_webhook_idempotency", columnList = "idempotency_key", unique = true),
    @Index(name = "idx_webhook_status", columnList = "status")
})
@Data
@Builder
public class WebhookEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;
    
    @Column(nullable = false)
    private String source; // BIFAST, QRIS, STRIPE, etc.
    
    @Column(nullable = false)
    private String eventType;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebhookStatus status;
    
    @Column(nullable = false)
    private Instant receivedAt;
    
    private Instant processedAt;
    
    @Version
    private Long version;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}

public enum WebhookStatus {
    PROCESSING,
    PROCESSED,
    FAILED,
    PERMANENTLY_FAILED
}
```

#### Webhook Best Practices

1. **Always verify signatures** - Use HMAC-SHA256 with timing-safe comparison
2. **Check timestamps** - Reject webhooks older than 5 minutes (replay protection)
3. **Implement idempotency** - Store processed webhook IDs to prevent duplicates
4. **Return 202 Accepted** - Acknowledge receipt immediately, process asynchronously
5. **Implement retries** - Use exponential backoff for failed webhooks
6. **Store raw payloads** - Keep audit trail of all received webhooks
7. **Alert on failures** - Notify ops team when webhooks fail permanently
8. **Use separate thread pool** - Don't block main request threads

```java
@Configuration
public class WebhookConfig {
    
    @Bean(name = "webhookExecutor")
    public Executor webhookExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("webhook-");
        executor.initialize();
        return executor;
    }
}
```

## Integration Patterns

### REST API Client Pattern

```javascript
class ServiceAPIClient {
  constructor(config) {
    this.apiKey = config.apiKey;
    this.baseURL = config.baseURL;
    this.timeout = config.timeout || 30000;
  }

  async request(method, endpoint, data = null) {
    const options = {
      method,
      headers: {
        'Authorization': `Bearer ${this.apiKey}`,
        'Content-Type': 'application/json'
      },
      timeout: this.timeout
    };

    if (data) {
      options.body = JSON.stringify(data);
    }

    const response = await retryWithBackoff(() =>
      fetch(`${this.baseURL}${endpoint}`, options)
    );

    return response.json();
  }

  // Resource methods
  async getResource(id) {
    return this.request('GET', `/resources/${id}`);
  }

  async createResource(data) {
    return this.request('POST', '/resources', data);
  }

  async updateResource(id, data) {
    return this.request('PUT', `/resources/${id}`, data);
  }

  async deleteResource(id) {
    return this.request('DELETE', `/resources/${id}`);
  }
}
```

### Pagination Handling

```javascript
async function* fetchAllPages(endpoint, pageSize = 100) {
  let cursor = null;

  do {
    const params = new URLSearchParams({
      limit: pageSize,
      ...(cursor && { cursor })
    });

    const response = await apiClient.request('GET', `${endpoint}?${params}`);

    yield response.data;

    cursor = response.pagination?.next_cursor;
  } while (cursor);
}

// Usage
for await (const page of fetchAllPages('/users')) {
  processUsers(page);
}
```

## Best Practices

### Security
- Store API keys in environment variables or secrets management
- Use HTTPS for all API calls
- Verify webhook signatures
- Implement request signing for sensitive operations
- Rotate API keys regularly

### Reliability
- Implement exponential backoff retry logic
- Handle rate limits gracefully
- Set appropriate timeouts
- Use circuit breakers for failing services
- Log all API interactions for debugging

### Performance
- Cache responses when appropriate
- Batch requests when the API supports it
- Use streaming for large responses
- Implement connection pooling
- Monitor API usage and costs

### Monitoring
- Track API response times
- Alert on error rate increases
- Monitor rate limit consumption
- Log failed requests with context
- Set up health checks for critical integrations

## Common Integration Examples

### Stripe Payment Processing
```javascript
const stripe = require('stripe')(process.env.STRIPE_SECRET_KEY);

async function createPaymentIntent(amount, currency = 'usd') {
  return await stripe.paymentIntents.create({
    amount,
    currency,
    automatic_payment_methods: { enabled: true }
  });
}
```

### SendGrid Email Sending
```javascript
const sgMail = require('@sendgrid/mail');
sgMail.setApiKey(process.env.SENDGRID_API_KEY);

async function sendEmail(to, subject, html) {
  await sgMail.send({
    to,
    from: process.env.FROM_EMAIL,
    subject,
    html
  });
}
```

### Twilio SMS
```javascript
const twilio = require('twilio')(
  process.env.TWILIO_ACCOUNT_SID,
  process.env.TWILIO_AUTH_TOKEN
);

async function sendSMS(to, body) {
  await twilio.messages.create({
    to,
    from: process.env.TWILIO_PHONE_NUMBER,
    body
  });
}
```

## Troubleshooting

### Authentication Issues
- Verify API keys are correctly set
- Check token expiration
- Ensure proper OAuth scopes
- Validate signature generation

### Rate Limiting
- Implement client-side rate limiting
- Use batch endpoints when available
- Spread requests over time
- Consider upgrading API tier

### Timeout Errors
- Increase timeout values for slow endpoints
- Implement request cancellation
- Use streaming for large payloads
- Check network connectivity

When integrating APIs, prioritize security, reliability, and maintainability. Always test error scenarios and edge cases before production deployment.
