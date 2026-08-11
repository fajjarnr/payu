# TokoBapak Integration — Client Implementation Guide

Contoh integrasi untuk client (TokoBapak) yang mengkonsumsi PayU Partner API.
Arsitektur partner gateway ada di [docs/architecture/ARCHITECTURE.md](../docs/architecture/ARCHITECTURE.md).

> Referensi API: `POST /v1/partner/auth/token` (client_credentials),
> `POST /v1/partner/payments`, webhook `X-Payu-Signature: sha256=...`.

## 10.3 Integration with payment-service

Update TokoBapak's `payment-service` to integrate with PayU:

```java
// PayU Client Configuration
@Configuration
public class PayuClientConfig {

    @Bean
    public PayuClient payuClient(
        @Value("${payu.base-url}") String baseUrl,
        @Value("${payu.client-id}") String clientId,
        @Value("${payu.client-secret}") String clientSecret
    ) {
        return PayuClient.builder()
            .baseUrl(baseUrl)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(30))
            .build();
    }
}

// PayU Payment Provider Implementation
@Service
@RequiredArgsConstructor
public class PayuPaymentProvider implements PaymentProvider {

    private final PayuClient payuClient;
    private final StreamBridge streamBridge;

    @Override
    public PaymentResult processPayment(ProcessPaymentRequest request) {
        // Create payment request to PayU
        PayuPaymentRequest payuRequest = PayuPaymentRequest.builder()
            .merchantReference(request.getOrderId())
            .amount(new Amount(request.getAmount(), "IDR"))
            .customer(mapCustomer(request))
            .paymentMethod("PAYU_BALANCE")
            .callbackUrl(webhookUrl)
            .build();

        PayuPaymentResponse response = payuClient.createPayment(payuRequest);

        return PaymentResult.builder()
            .paymentId(response.getPaymentId())
            .status(PaymentStatus.PENDING)
            .paymentUrl(response.getPaymentUrl())
            .build();
    }

    // Webhook handler for PayU callbacks
    @PostMapping("/webhooks/payu")
    public ResponseEntity<Void> handlePayuWebhook(
        @RequestHeader("X-Payu-Signature") String signature,
        @RequestBody PayuWebhookEvent event
    ) {
        // Verify signature
        if (!payuClient.verifySignature(signature, event)) {
            return ResponseEntity.status(401).build();
        }

        // Publish event to Kafka
        PaymentProcessedEvent processedEvent = PaymentProcessedEvent.builder()
            .paymentId(event.getPaymentId())
            .orderId(event.getMerchantReference())
            .status(mapStatus(event.getStatus()))
            .transactionId(event.getTransactionId())
            .amount(event.getAmount().getValue())
            .build();

        streamBridge.send("paymentEvents-out-0", processedEvent);

        return ResponseEntity.ok().build();
    }
}
```

## 10.4 SDK Design (Optional)

> Catatan: `payu-java-sdk` belum dipublikasikan; contoh di bawah adalah bentuk
> target API.

```xml
<!-- Maven Dependency -->
<dependency>
    <groupId>id.payu</groupId>
    <artifactId>payu-java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
// Usage Example
PayuClient payu = PayuClient.builder()
    .apiKey("pk_live_xxxxx")
    .secretKey("sk_live_xxxxx")
    .build();

// Create payment
Payment payment = payu.payments().create(
    CreatePaymentRequest.builder()
        .merchantReference("ORDER-123")
        .amount(150000L)
        .currency("IDR")
        .customerPhone("+6281234567890")
        .build()
);

// Check status
Payment status = payu.payments().get(payment.getId());
```
