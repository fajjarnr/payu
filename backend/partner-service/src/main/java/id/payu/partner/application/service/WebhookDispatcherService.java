package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.repository.WebhookDeliveryRepository;
import id.payu.partner.adapter.persistence.repository.WebhookSubscriptionRepository;
import id.payu.partner.adapter.persistence.entity.WebhookDeliveryEntity;
import id.payu.partner.adapter.persistence.entity.WebhookSubscriptionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import id.payu.partner.domain.Status;

/**
 * Dispatches webhook events to partner endpoints.
 * <p>
 * Features:
 * - HMAC-SHA256 payload signing
 * - Exponential backoff retry (30s, 2m, 8m, 32m, 2h)
 * - Scheduled retry of failed deliveries
 * - Delivery log tracking
 * - Configurable max retries per subscription
 */
@Service
public class WebhookDispatcherService {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcherService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MAX_RESPONSE_BODY_BYTES = 64 * 1024;

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookUrlValidatorService webhookUrlValidator;
    private final HttpClient httpClient;
    private final java.util.List<id.payu.api.common.webhook.WebhookHandler> webhookHandlers;

    @Autowired
    public WebhookDispatcherService(WebhookSubscriptionRepository subscriptionRepository,
                                    WebhookDeliveryRepository deliveryRepository,
                                    WebhookUrlValidatorService webhookUrlValidator,
                                    org.springframework.beans.factory.ObjectProvider<java.util.List<id.payu.api.common.webhook.WebhookHandler>> webhookHandlersProvider) {
        this(subscriptionRepository, deliveryRepository, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build(), webhookUrlValidator,
                webhookHandlersProvider.getIfAvailable(java.util.Collections::emptyList));
    }

    // Visible for testing
    WebhookDispatcherService(WebhookSubscriptionRepository subscriptionRepository,
                             WebhookDeliveryRepository deliveryRepository,
                             HttpClient httpClient,
                             WebhookUrlValidatorService webhookUrlValidator,
                             java.util.List<id.payu.api.common.webhook.WebhookHandler> webhookHandlers) {
        this.subscriptionRepository = subscriptionRepository;
        this.deliveryRepository = deliveryRepository;
        this.webhookUrlValidator = webhookUrlValidator;
        this.httpClient = httpClient;
        this.webhookHandlers = webhookHandlers != null ? webhookHandlers : java.util.List.of();
    }

    /**
     * Dispatch an event to all matching webhook subscriptions.
     * Creates delivery records and attempts immediate delivery asynchronously.
     *
     * @param eventType e.g., "payment.completed"
     * @param payload   JSON payload to send
     */
    @Async
    public void dispatch(String eventType, Map<String, Object> payload) {
        String eventId = "evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        dispatch(eventType, eventId, payload);
    }

    /**
     * Dispatch an event with explicit event ID (for idempotency).
     *
     * <p>BUG-WEBHOOK-ASYNC-001: Removed {@code @Transactional} from this {@code @Async} method.
     * Per BUG-BE-049 lesson: {@code @Transactional} is a no-op on {@code @Async} methods because
     * the proxy is only applied at the call site, not on the async thread. Each {@code repository.save()}
     * runs in its own implicit transaction. Multiple {@code deliveryRepository.save()} calls in the
     * subscription loop below would NOT be rolled back together on failure (partial state).
     */
    @Async
    public void dispatch(String eventType, String eventId, Map<String, Object> payload) {
        List<WebhookSubscriptionEntity> subscriptions =
                subscriptionRepository.findActiveByEventType(eventType);

        // Build the webhook payload envelope
        Map<String, Object> envelope = buildEnvelope(eventId, eventType, payload);
        String payloadJson = toJson(envelope);

        // ARCH-PARTNER-001: route the event to internal handlers (e.g. payment
        // state updates) whose supportedEventTypes match, regardless of whether
        // any external webhook subscription exists.
        for (id.payu.api.common.webhook.WebhookHandler handler : webhookHandlers) {
            String[] supportedTypes = handler.supportedEventTypes();
            if (supportedTypes == null) {
                continue;
            }
            for (String supported : supportedTypes) {
                if (supported.equals(eventType)) {
                    try {
                        handler.processWebhook(eventId, payloadJson);
                        handler.onSuccess(eventId, envelope);
                    } catch (Exception e) {
                        log.error("Webhook handler {} failed for event {}: {}",
                                handler.getClass().getSimpleName(), eventId, e.getMessage());
                        handler.onError(eventId, e);
                    }
                    break;
                }
            }
        }

        for (WebhookSubscriptionEntity subscription : subscriptions) {
            if (!subscription.matchesEvent(eventType)) continue;

            // MVP-006: idempotency — skip if this event was already dispatched to this subscription.
            // Outbox is at-least-once (a re-consumed event can re-enter dispatch), so a duplicate
            // dispatch must not create a second delivery row nor re-send the webhook.
            // Backed by unique index uq_webhook_delivery_event (V16).
            if (deliveryRepository.existsByEventIdAndSubscription_Id(eventId, subscription.getId())) {
                log.info("Idempotent skip: event {} already delivered to subscription {}", eventId, subscription.getId());
                continue;
            }

            WebhookDeliveryEntity delivery = new WebhookDeliveryEntity(
                    subscription, eventId, eventType, payloadJson);
            delivery = deliveryRepository.save(delivery);

            attemptDelivery(delivery, subscription);
        }
    }

    /**
     * Retry failed deliveries that are due for retry.
     * Scheduled every 30 seconds.
     */
    @SchedulerLock(name = "WebhookDispatcherService_retryFailedDeliveries", lockAtLeastFor = "PT1S", lockAtMostFor = "PT1M")@Scheduled(fixedDelay = 30000)
    @Transactional
    public void retryFailedDeliveries() {
        try {
            List<WebhookDeliveryEntity> retryable =
                    deliveryRepository.findRetryableDeliveries(LocalDateTime.now());

            if (retryable.isEmpty()) return;

            log.info("Retrying {} failed webhook deliveries", retryable.size());

            for (WebhookDeliveryEntity delivery : retryable) {
                WebhookSubscriptionEntity subscription = delivery.getSubscription();
                if (!subscription.isActive()) {
                    delivery.setStatus(Status.EXHAUSTED);
                    delivery.setErrorMessage("Subscription deactivated");
                    deliveryRepository.save(delivery);
                    continue;
                }
                attemptDelivery(delivery, subscription);
            }
        } catch (Exception e) {
            log.error("Unexpected error occurred during webhook retry scheduled task", e);
        }
    }

    /**
     * Clean up old delivery records (90 day retention).
     * Runs daily at 3 AM.
     */
    @SchedulerLock(name = "WebhookDispatcherService_cleanupOldDeliveries", lockAtLeastFor = "PT1S", lockAtMostFor = "PT4H")@Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldDeliveries() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
            int deleted = deliveryRepository.deleteOldDeliveries(cutoff);
            if (deleted > 0) {
                log.info("Cleaned up {} old webhook delivery records (before {})", deleted, cutoff);
            }
        } catch (Exception e) {
            log.error("Unexpected error occurred during webhook cleanup scheduled task", e);
        }
    }

    /**
     * Attempt to deliver a webhook payload to the partner endpoint.
     */
    void attemptDelivery(WebhookDeliveryEntity delivery, WebhookSubscriptionEntity subscription) {
        delivery.markDelivering();
        persistState(delivery);

        try {
            // PARTNER-PROD-003: re-validate the resolved endpoint before every
            // attempt so a URL that was public at create time but rebinds to an
            // internal address (or was written to the DB directly) is still blocked
            // at the last check before the socket opens.
            webhookUrlValidator.validate(subscription.getUrl());

            String payload = delivery.getPayload();
            String timestamp = Instant.now().toString();
            String signature = computeHmac(subscription.getSecret(), timestamp + "." + payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(subscription.getUrl()))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "PayU-Webhook/1.0")
                    .header("X-PayU-Event", delivery.getEventType())
                    .header("X-PayU-Delivery-Id", String.valueOf(delivery.getId()))
                    .header("X-PayU-Event-Id", delivery.getEventId())
                    .header("X-PayU-Timestamp", timestamp)
                    .header("X-PayU-Signature", "sha256=" + signature)
                    .timeout(Duration.ofSeconds(15))
                    .build();

            // PARTNER-PROD-003: bound the response body so a webhook endpoint
            // cannot exhaust memory; the truncated prefix is stored for diagnostics.
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.limiting(
                            HttpResponse.BodyHandlers.ofString(), MAX_RESPONSE_BODY_BYTES));

            int status = response.statusCode();
            String body = response.body();
            if (status >= 200 && status < 300) {
                delivery.markDelivered(status, body);
                log.info("Webhook delivered: {} -> {} (HTTP {})",
                        delivery.getEventId(), subscription.getUrl(), status);
            } else {
                delivery.markFailed(status, body,
                        "Non-2xx response: " + status);
                log.warn("Webhook delivery failed: {} -> {} (HTTP {}, attempt {}/{})",
                        delivery.getEventId(), subscription.getUrl(), status,
                        delivery.getAttemptCount(), delivery.getMaxAttempts());
            }
        } catch (IllegalArgumentException e) {
            // Trust-boundary rejection: do not schedule further retries for a
            // URL that can never be delivered to.
            delivery.markFailed(null, null, "Webhook URL blocked: " + e.getMessage());
            log.warn("Webhook delivery blocked: {} -> {} ({})",
                    delivery.getEventId(), subscription.getUrl(), e.getMessage());
        } catch (Exception e) {
            delivery.markFailed(null, null, e.getMessage());
            log.error("Webhook delivery error: {} -> {} ({}, attempt {}/{})",
                    delivery.getEventId(), subscription.getUrl(), e.getMessage(),
                    delivery.getAttemptCount(), delivery.getMaxAttempts());
        }

        persistState(delivery);
    }

    /**
     * PARTNER-PROD-004: persist a delivery state transition without losing the
     * terminal state to a concurrent dispatcher's optimistic-lock update. On
     * conflict the row is reloaded and this attempt's transition is re-applied
     * to the fresh version, so a racing duplicate can never leave a delivery
     * stuck in a transient state.
     */
    private void persistState(WebhookDeliveryEntity delivery) {
        try {
            deliveryRepository.save(delivery);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            deliveryRepository.findById(delivery.getId()).ifPresent(current -> {
                if (current.getStatus() == Status.PENDING) {
                    current.markDelivering();
                }
                if (delivery.getStatus() == Status.DELIVERED) {
                    current.markDelivered(delivery.getResponseCode(), delivery.getResponseBody());
                } else if (delivery.getStatus() == Status.FAILED || delivery.getStatus() == Status.EXHAUSTED) {
                    current.markFailed(delivery.getResponseCode(), delivery.getResponseBody(),
                            delivery.getErrorMessage());
                }
                deliveryRepository.save(current);
            });
            log.warn("Reconciled delivery {} after optimistic-lock conflict (state={})",
                    delivery.getId(), delivery.getStatus());
        }
    }

    /**
     * Compute HMAC-SHA256 signature.
     * PartnerEntity verifies: HMAC-SHA256(secret, timestamp + "." + body) == signature
     */
    String computeHmac(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC signature", e);
        }
    }

    private Map<String, Object> buildEnvelope(String eventId, String eventType,
                                               Map<String, Object> payload) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("id", eventId);
        envelope.put("type", eventType);
        envelope.put("created", Instant.now().toString());
        envelope.put("data", payload);
        return envelope;
    }

    /**
     * Simple JSON serialization without external dependency.
     * For production, consider using Jackson ObjectMapper.
     */
    private String toJson(Map<String, Object> map) {
        try {
            // Use Jackson if available via class loader
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.findAndRegisterModules();
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Failed to serialize webhook payload to JSON", e);
            return "{}";
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
