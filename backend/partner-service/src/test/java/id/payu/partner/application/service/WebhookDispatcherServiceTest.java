package id.payu.partner.application.service;

import id.payu.partner.adapter.persistence.repository.WebhookDeliveryRepository;
import id.payu.partner.adapter.persistence.repository.WebhookSubscriptionRepository;
import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import id.payu.partner.adapter.persistence.entity.WebhookDeliveryEntity;
import id.payu.partner.adapter.persistence.entity.WebhookSubscriptionEntity;
import id.payu.partner.domain.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookDispatcherServiceTest {

    @Mock
    private WebhookSubscriptionRepository subscriptionRepository;

    @Mock
    private WebhookDeliveryRepository deliveryRepository;

    @Mock
    private HttpClient httpClient;

    private WebhookDispatcherService dispatcher;
    private id.payu.api.common.webhook.WebhookHandler webhookHandler;
    private PartnerEntity partner;
    private WebhookSubscriptionEntity subscription;

    @BeforeEach
    void setUp() {
        webhookHandler = org.mockito.Mockito.mock(id.payu.api.common.webhook.WebhookHandler.class);
        dispatcher = new WebhookDispatcherService(subscriptionRepository, deliveryRepository,
                httpClient, new WebhookUrlValidatorService(), java.util.List.of(webhookHandler));

        partner = new PartnerEntity();
        partner.setId(1L);
        partner.setName("TokoBapak");
        partner.setActive(true);
        partner.setStatus(id.payu.partner.domain.PartnerStatus.ACTIVE);

        subscription = new WebhookSubscriptionEntity(
                partner,
                "https://8.8.8.8/webhooks",
                "payment.completed,payment.failed",
                "whsec_test_secret_123"
        );
        subscription.setId(10L);
        subscription.setMaxRetries(3);
    }

    @Nested
    @DisplayName("HMAC Signature Generation")
    class HmacSignature {

        @Test
        @DisplayName("should compute consistent HMAC-SHA256 signature")
        void shouldComputeConsistentHmac() {
            String secret = "test_secret";
            String data = "2024-01-01T00:00:00Z.{\"id\":\"evt_123\",\"type\":\"test\"}";

            String sig1 = dispatcher.computeHmac(secret, data);
            String sig2 = dispatcher.computeHmac(secret, data);

            assertNotNull(sig1);
            assertEquals(64, sig1.length(), "HMAC-SHA256 should produce 64 hex chars");
            assertEquals(sig1, sig2, "Same input must produce same signature");
        }

        @Test
        @DisplayName("should produce different signatures for different payloads")
        void shouldProduceDifferentSignaturesForDifferentData() {
            String secret = "test_secret";
            String sig1 = dispatcher.computeHmac(secret, "payload_1");
            String sig2 = dispatcher.computeHmac(secret, "payload_2");

            assertNotEquals(sig1, sig2);
        }

        @Test
        @DisplayName("should produce different signatures for different secrets")
        void shouldProduceDifferentSignaturesForDifferentSecrets() {
            String sig1 = dispatcher.computeHmac("secret_a", "same_data");
            String sig2 = dispatcher.computeHmac("secret_b", "same_data");

            assertNotEquals(sig1, sig2);
        }
    }

    @Nested
    @DisplayName("Event Dispatch")
    class EventDispatch {

    @Test
    @DisplayName("should invoke matching webhook handler on dispatch")
    void shouldInvokeMatchingWebhookHandlerOnDispatch() throws Exception {
        org.mockito.Mockito.when(webhookHandler.supportedEventTypes())
                .thenReturn(new String[]{"payment.completed"});
        subscriptionRepository.deleteAll();
        dispatcher.dispatch("payment.completed", "evt_handler001", Map.of("amount", 1000));

        org.mockito.Mockito.verify(webhookHandler).processWebhook(
                org.mockito.ArgumentMatchers.eq("evt_handler001"),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("should skip dispatch when no subscriptions match")
    void shouldSkipWhenNoSubscriptions() {
            when(subscriptionRepository.findActiveByEventType("payment.refunded"))
                    .thenReturn(List.of());

            dispatcher.dispatch("payment.refunded", "evt_123", Map.of("amount", 1000));

            verify(deliveryRepository, never()).save(any());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should create delivery record and attempt delivery")
        void shouldCreateDeliveryAndAttempt() throws Exception {
            when(subscriptionRepository.findActiveByEventType("payment.completed"))
                    .thenReturn(List.of(subscription));
            when(deliveryRepository.save(any(WebhookDeliveryEntity.class)))
                    .thenAnswer(inv -> {
                        WebhookDeliveryEntity d = inv.getArgument(0);
                        d.setId(200L);
                        return d;
                    });

            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("OK");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            dispatcher.dispatch("payment.completed", "evt_test001",
                    Map.of("amount", 50000, "currency", "IDR"));

            // Verify delivery was saved (initial + after attempt)
            ArgumentCaptor<WebhookDeliveryEntity> captor =
                    ArgumentCaptor.forClass(WebhookDeliveryEntity.class);
            verify(deliveryRepository, atLeast(2)).save(captor.capture());

            List<WebhookDeliveryEntity> saved = captor.getAllValues();
            // First save = PENDING, second = DELIVERING, third = DELIVERED
            WebhookDeliveryEntity finalState = saved.get(saved.size() - 1);
            assertEquals(Status.DELIVERED, finalState.getStatus());
            assertEquals(200, finalState.getResponseCode());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should mark delivery as FAILED on non-2xx response")
        void shouldMarkFailedOnNon2xx() throws Exception {
            when(subscriptionRepository.findActiveByEventType("payment.completed"))
                    .thenReturn(List.of(subscription));
            when(deliveryRepository.save(any(WebhookDeliveryEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode()).thenReturn(500);
            when(mockResponse.body()).thenReturn("Internal Server Error");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            dispatcher.dispatch("payment.completed", "evt_fail001", Map.of("test", true));

            ArgumentCaptor<WebhookDeliveryEntity> captor =
                    ArgumentCaptor.forClass(WebhookDeliveryEntity.class);
            verify(deliveryRepository, atLeast(2)).save(captor.capture());

            WebhookDeliveryEntity finalState = captor.getAllValues().get(captor.getAllValues().size() - 1);
            assertEquals(Status.FAILED, finalState.getStatus());
            assertEquals(500, finalState.getResponseCode());
            assertNotNull(finalState.getNextRetryAt(), "Should schedule retry");
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should mark delivery as FAILED on connection error")
        void shouldMarkFailedOnConnectionError() throws Exception {
            when(subscriptionRepository.findActiveByEventType("payment.completed"))
                    .thenReturn(List.of(subscription));
            when(deliveryRepository.save(any(WebhookDeliveryEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new java.io.IOException("Connection refused"));

            dispatcher.dispatch("payment.completed", "evt_err001", Map.of("test", true));

            ArgumentCaptor<WebhookDeliveryEntity> captor =
                    ArgumentCaptor.forClass(WebhookDeliveryEntity.class);
            verify(deliveryRepository, atLeast(2)).save(captor.capture());

            WebhookDeliveryEntity finalState = captor.getAllValues().get(captor.getAllValues().size() - 1);
            assertEquals(Status.FAILED, finalState.getStatus());
            assertNotNull(finalState.getErrorMessage());
            assertTrue(finalState.getErrorMessage().contains("Connection refused"));
        }

        @Test
        @DisplayName("should skip re-dispatch when event already delivered to subscription (MVP-006)")
        void shouldSkipDuplicateEvent() throws Exception {
            when(subscriptionRepository.findActiveByEventType("payment.completed"))
                    .thenReturn(List.of(subscription));
            // Existing delivery for this event+subscription → dedup guard skips creation/send.
            when(deliveryRepository.existsByEventIdAndSubscription_Id("evt_dup001", 10L)).thenReturn(true);

            dispatcher.dispatch("payment.completed", "evt_dup001", Map.of("test", true));

            verify(deliveryRepository, never()).save(any(WebhookDeliveryEntity.class));
            verify(httpClient, never()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should block delivery to a non-public URL without making an HTTP call (PARTNER-PROD-003)")
        void shouldBlockPrivateUrlDelivery() throws Exception {
            subscription.setUrl("https://169.254.169.254/latest/meta-data");

            when(subscriptionRepository.findActiveByEventType("payment.completed"))
                    .thenReturn(List.of(subscription));
            when(deliveryRepository.save(any(WebhookDeliveryEntity.class)))
                    .thenAnswer(inv -> {
                        WebhookDeliveryEntity d = inv.getArgument(0);
                        d.setId(250L);
                        return d;
                    });

            dispatcher.dispatch("payment.completed", "evt_ssrf001", Map.of("test", true));

            ArgumentCaptor<WebhookDeliveryEntity> captor =
                    ArgumentCaptor.forClass(WebhookDeliveryEntity.class);
            verify(deliveryRepository, atLeast(2)).save(captor.capture());

            WebhookDeliveryEntity finalState = captor.getAllValues().get(captor.getAllValues().size() - 1);
            assertEquals(Status.FAILED, finalState.getStatus());
            assertNotNull(finalState.getErrorMessage());
            assertTrue(finalState.getErrorMessage().contains("non-public address"),
                    "error should explain the SSRF block, was: " + finalState.getErrorMessage());
            verify(httpClient, never()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should reconcile terminal state when final save hits an optimistic-lock conflict (PARTNER-PROD-004)")
        void shouldReconcileOnOptimisticLockConflict() throws Exception {
            subscription.setUrl("https://8.8.8.8/webhooks");

            when(subscriptionRepository.findActiveByEventType("payment.completed"))
                    .thenReturn(List.of(subscription));
            java.util.concurrent.atomic.AtomicBoolean conflictThrown = new java.util.concurrent.atomic.AtomicBoolean(false);
            when(deliveryRepository.save(any(WebhookDeliveryEntity.class)))
                    .thenAnswer(inv -> {
                        WebhookDeliveryEntity d = inv.getArgument(0);
                        if (d.getId() == null) {
                            d.setId(300L);
                        } else if (d.getStatus() == Status.DELIVERED
                                && conflictThrown.compareAndSet(false, true)) {
                            throw new org.springframework.orm.ObjectOptimisticLockingFailureException(
                                    WebhookDeliveryEntity.class, 300L);
                        }
                        return d;
                    });
            WebhookDeliveryEntity persistedRow = new WebhookDeliveryEntity(
                    subscription, "evt_opt001", "payment.completed", "{\"test\":true}");
            persistedRow.setId(300L);
            when(deliveryRepository.findById(300L)).thenReturn(java.util.Optional.of(persistedRow));

            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("OK");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            dispatcher.dispatch("payment.completed", "evt_opt001", Map.of("test", true));

            ArgumentCaptor<WebhookDeliveryEntity> captor =
                    ArgumentCaptor.forClass(WebhookDeliveryEntity.class);
            verify(deliveryRepository, atLeast(3)).save(captor.capture());
            WebhookDeliveryEntity reconciled = captor.getAllValues().get(captor.getAllValues().size() - 1);
            assertEquals(Status.DELIVERED, reconciled.getStatus());
            assertEquals(200, reconciled.getResponseCode());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should include correct headers in webhook request")
        void shouldIncludeCorrectHeaders() throws Exception {
            when(subscriptionRepository.findActiveByEventType("payment.completed"))
                    .thenReturn(List.of(subscription));
            when(deliveryRepository.save(any(WebhookDeliveryEntity.class)))
                    .thenAnswer(inv -> {
                        WebhookDeliveryEntity d = inv.getArgument(0);
                        d.setId(300L);
                        return d;
                    });

            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("OK");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            dispatcher.dispatch("payment.completed", "evt_hdr001", Map.of("test", true));

            ArgumentCaptor<HttpRequest> requestCaptor =
                    ArgumentCaptor.forClass(HttpRequest.class);
            verify(httpClient).send(requestCaptor.capture(), any());

            HttpRequest request = requestCaptor.getValue();
            assertEquals("https://8.8.8.8/webhooks",
                    request.uri().toString());
            assertTrue(request.headers().firstValue("Content-Type")
                    .orElse("").contains("application/json"));
            assertTrue(request.headers().firstValue("X-PayU-Event")
                    .orElse("").equals("payment.completed"));
            assertTrue(request.headers().firstValue("X-PayU-Signature")
                    .orElse("").startsWith("sha256="));
            assertTrue(request.headers().firstValue("X-PayU-Event-Id")
                    .orElse("").equals("evt_hdr001"));
            assertTrue(request.headers().firstValue("X-PayU-Timestamp")
                    .isPresent());
            assertEquals("PayU-Webhook/1.0",
                    request.headers().firstValue("User-Agent").orElse(""));
        }
    }

    @Nested
    @DisplayName("Retry Processing")
    class RetryProcessing {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should retry failed deliveries that are due")
        void shouldRetryFailedDeliveries() throws Exception {
            WebhookDeliveryEntity failedDelivery = new WebhookDeliveryEntity(
                    subscription, "evt_retry001", "payment.completed", "{\"retry\":true}");
            failedDelivery.setId(500L);
            failedDelivery.setStatus(Status.FAILED);
            failedDelivery.setAttemptCount(1);
            failedDelivery.setNextRetryAt(LocalDateTime.now().minusMinutes(1));

            when(deliveryRepository.findRetryableDeliveries(any(LocalDateTime.class)))
                    .thenReturn(List.of(failedDelivery));
            when(deliveryRepository.save(any(WebhookDeliveryEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("OK");
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            dispatcher.retryFailedDeliveries();

            ArgumentCaptor<WebhookDeliveryEntity> captor =
                    ArgumentCaptor.forClass(WebhookDeliveryEntity.class);
            verify(deliveryRepository, atLeast(2)).save(captor.capture());

            WebhookDeliveryEntity finalState = captor.getAllValues().get(captor.getAllValues().size() - 1);
            assertEquals(Status.DELIVERED, finalState.getStatus());
        }

        @Test
        @DisplayName("should mark EXHAUSTED for deactivated subscriptions during retry")
        void shouldExhaustForDeactivatedSubscription() {
            subscription.setActive(false);

            WebhookDeliveryEntity failedDelivery = new WebhookDeliveryEntity(
                    subscription, "evt_deact001", "payment.completed", "{\"test\":true}");
            failedDelivery.setId(600L);
            failedDelivery.setStatus(Status.FAILED);
            failedDelivery.setAttemptCount(1);
            failedDelivery.setNextRetryAt(LocalDateTime.now().minusMinutes(1));

            when(deliveryRepository.findRetryableDeliveries(any(LocalDateTime.class)))
                    .thenReturn(List.of(failedDelivery));
            when(deliveryRepository.save(any(WebhookDeliveryEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            dispatcher.retryFailedDeliveries();

            ArgumentCaptor<WebhookDeliveryEntity> captor =
                    ArgumentCaptor.forClass(WebhookDeliveryEntity.class);
            verify(deliveryRepository).save(captor.capture());
            assertEquals(Status.EXHAUSTED, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("should skip retry when no deliveries are due")
        void shouldSkipRetryWhenNoneAvailable() {
            when(deliveryRepository.findRetryableDeliveries(any(LocalDateTime.class)))
                    .thenReturn(List.of());

            dispatcher.retryFailedDeliveries();

            verify(deliveryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delivery Cleanup")
    class DeliveryCleanup {

        @Test
        @DisplayName("should clean up old delivery records")
        void shouldCleanupOldDeliveries() {
            when(deliveryRepository.deleteOldDeliveries(any(LocalDateTime.class))).thenReturn(42);

            dispatcher.cleanupOldDeliveries();

            verify(deliveryRepository).deleteOldDeliveries(any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("Domain Model - WebhookDeliveryEntity")
    class DeliveryDomainModel {

        @Test
        @DisplayName("should transition through delivery lifecycle")
        void shouldTransitionThroughLifecycle() {
            WebhookDeliveryEntity delivery = new WebhookDeliveryEntity(
                    subscription, "evt_lc001", "payment.completed", "{\"test\":true}");

            assertEquals(Status.PENDING, delivery.getStatus());
            assertEquals(0, delivery.getAttemptCount());

            delivery.markDelivering();
            assertEquals(Status.DELIVERING, delivery.getStatus());

            delivery.markDelivered(200, "OK");
            assertEquals(Status.DELIVERED, delivery.getStatus());
            assertEquals(200, delivery.getResponseCode());
            assertNotNull(delivery.getDeliveredAt());
            assertEquals(1, delivery.getAttemptCount());
        }

        @Test
        @DisplayName("should calculate exponential backoff for retries")
        void shouldCalculateExponentialBackoff() {
            WebhookDeliveryEntity delivery = new WebhookDeliveryEntity(
                    subscription, "evt_bo001", "payment.completed", "{\"test\":true}");

            // First failure: 30s backoff
            delivery.markFailed(500, "Error", "Server Error");
            assertEquals(Status.FAILED, delivery.getStatus());
            assertEquals(1, delivery.getAttemptCount());
            assertNotNull(delivery.getNextRetryAt());
            assertTrue(delivery.canRetry());
        }

        @Test
        @DisplayName("should mark EXHAUSTED when max attempts exceeded")
        void shouldExhaustAfterMaxAttempts() {
            WebhookDeliveryEntity delivery = new WebhookDeliveryEntity(
                    subscription, "evt_ex001", "payment.completed", "{\"test\":true}");
            delivery.setMaxAttempts(2);

            delivery.markFailed(500, "Error 1", "Server Error");
            assertEquals(Status.FAILED, delivery.getStatus());
            assertTrue(delivery.canRetry());

            delivery.markFailed(500, "Error 2", "Server Error");
            assertEquals(Status.EXHAUSTED, delivery.getStatus());
            assertFalse(delivery.canRetry());
            assertNull(delivery.getNextRetryAt());
        }
    }

    @Nested
    @DisplayName("Domain Model - WebhookSubscriptionEntity")
    class SubscriptionDomainModel {

        @Test
        @DisplayName("should match specific event types")
        void shouldMatchSpecificEvents() {
            assertTrue(subscription.matchesEvent("payment.completed"));
            assertTrue(subscription.matchesEvent("payment.failed"));
            assertFalse(subscription.matchesEvent("payment.refunded"));
        }

        @Test
        @DisplayName("should match wildcard subscription")
        void shouldMatchWildcard() {
            WebhookSubscriptionEntity wildcardSub = new WebhookSubscriptionEntity(
                    partner, "https://example.com/wh", "*", "secret");
            wildcardSub.setId(20L);

            assertTrue(wildcardSub.matchesEvent("payment.completed"));
            assertTrue(wildcardSub.matchesEvent("any.random.event"));
        }

        @Test
        @DisplayName("should not match when inactive")
        void shouldNotMatchWhenInactive() {
            subscription.setActive(false);
            assertFalse(subscription.matchesEvent("payment.completed"));
        }
    }
}
