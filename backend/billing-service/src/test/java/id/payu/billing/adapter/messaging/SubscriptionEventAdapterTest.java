package id.payu.billing.adapter.messaging;

import id.payu.billing.domain.event.SubscriptionEvent;
import id.payu.billing.adapter.persistence.entity.SubscriptionEntity;
import id.payu.billing.adapter.persistence.entity.SubscriptionChargeEntity;
import id.payu.events.cloudevents.CloudEventEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionEventAdapter Unit Tests")
class SubscriptionEventAdapterTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private SubscriptionEventAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SubscriptionEventAdapter(kafkaTemplate);
    }

    @Test
    @DisplayName("should publish subscription.created event to correct topic")
    void shouldPublishSubscriptionCreatedEvent() {
        // Given
        SubscriptionEntity subscription = createSampleSubscription();
        CompletableFuture future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(Message.class))).thenReturn(future);

        // When
        adapter.publishSubscriptionCreated(subscription);

        // Then
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message message = messageCaptor.getValue();
        assertEquals(SubscriptionEvent.TOPIC, message.getHeaders().get(KafkaHeaders.TOPIC));
        assertEquals(subscription.getId().toString(), message.getHeaders().get(KafkaHeaders.KEY));
        assertEquals(SubscriptionEvent.SUBSCRIPTION_CREATED, message.getHeaders().get("X-Event-Type"));
        assertEquals(subscription.getPartnerId(), message.getHeaders().get("X-Partner-Id"));

        CloudEventEnvelope payload = (CloudEventEnvelope) message.getPayload();
        assertNotNull(payload);
        assertEquals(SubscriptionEvent.SUBSCRIPTION_CREATED, payload.getType());
    }

    @Test
    @DisplayName("should publish charge.succeeded event to correct topic")
    void shouldPublishChargeSucceededEvent() {
        // Given
        SubscriptionEntity subscription = createSampleSubscription();
        SubscriptionChargeEntity charge = createSampleCharge(subscription.getId(), true);
        CompletableFuture future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(Message.class))).thenReturn(future);

        // When
        adapter.publishChargeSucceeded(subscription, charge);

        // Then
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message message = messageCaptor.getValue();
        assertEquals(SubscriptionEvent.TOPIC, message.getHeaders().get(KafkaHeaders.TOPIC));
        assertEquals(charge.getId().toString(), message.getHeaders().get(KafkaHeaders.KEY));
        assertEquals(SubscriptionEvent.CHARGE_SUCCEEDED, message.getHeaders().get("X-Event-Type"));
        assertEquals(subscription.getPartnerId(), message.getHeaders().get("X-Partner-Id"));
        assertEquals(subscription.getId().toString(), message.getHeaders().get("X-SubscriptionEntity-Id"));

        CloudEventEnvelope payload = (CloudEventEnvelope) message.getPayload();
        assertNotNull(payload);
        assertEquals(SubscriptionEvent.CHARGE_SUCCEEDED, payload.getType());
    }

    @Test
    @DisplayName("should publish charge.failed event to correct topic")
    void shouldPublishChargeFailedEvent() {
        // Given
        SubscriptionEntity subscription = createSampleSubscription();
        SubscriptionChargeEntity charge = createSampleCharge(subscription.getId(), false);
        charge.markFailed("Insufficient balance");
        CompletableFuture future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(Message.class))).thenReturn(future);

        // When
        adapter.publishChargeFailed(subscription, charge);

        // Then
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message message = messageCaptor.getValue();
        assertEquals(SubscriptionEvent.TOPIC, message.getHeaders().get(KafkaHeaders.TOPIC));
        assertEquals(charge.getId().toString(), message.getHeaders().get(KafkaHeaders.KEY));
        assertEquals(SubscriptionEvent.CHARGE_FAILED, message.getHeaders().get("X-Event-Type"));
        assertEquals(subscription.getPartnerId(), message.getHeaders().get("X-Partner-Id"));
        assertEquals(subscription.getId().toString(), message.getHeaders().get("X-SubscriptionEntity-Id"));

        CloudEventEnvelope payload = (CloudEventEnvelope) message.getPayload();
        assertNotNull(payload);
        assertEquals(SubscriptionEvent.CHARGE_FAILED, payload.getType());
    }

    @Test
    @DisplayName("should handle Kafka publish failure gracefully")
    void shouldHandlePublishFailure() {
        // Given
        SubscriptionEntity subscription = createSampleSubscription();
        CompletableFuture future = new CompletableFuture();
        future.completeExceptionally(new RuntimeException("Kafka unavailable"));
        when(kafkaTemplate.send(any(Message.class))).thenReturn(future);

        // When - should not throw
        assertDoesNotThrow(() -> adapter.publishSubscriptionCreated(subscription));

        // Then
        verify(kafkaTemplate).send(any(Message.class));
    }

    // Helper methods

    private SubscriptionEntity createSampleSubscription() {
        SubscriptionEntity sub = new SubscriptionEntity();
        sub.setId(UUID.randomUUID());
        sub.setAccountId("acc-123456");
        sub.setPlanId(UUID.randomUUID());
        sub.setPartnerId("partner-nobar");
        sub.setStatus(SubscriptionEntity.SubscriptionStatus.ACTIVE);
        sub.setCurrentPrice(new BigDecimal("99000"));
        sub.setCurrency("IDR");
        sub.setExternalReferenceId("ext-ref-001");
        sub.setCreatedAt(LocalDateTime.now());
        return sub;
    }

    private SubscriptionChargeEntity createSampleCharge(UUID subscriptionId, boolean succeeded) {
        SubscriptionChargeEntity charge = new SubscriptionChargeEntity();
        charge.setId(UUID.randomUUID());
        charge.setSubscriptionId(subscriptionId);
        charge.setAccountId("acc-123456");
        charge.setAmount(new BigDecimal("99000"));
        charge.setCurrency("IDR");
        charge.setAttemptNumber(1);
        charge.setBillingPeriodStart(LocalDateTime.now().minusMonths(1));
        charge.setBillingPeriodEnd(LocalDateTime.now());

        if (succeeded) {
            charge.markSucceeded();
        } else {
            charge.setStatus(SubscriptionChargeEntity.ChargeStatus.FAILED);
        }

        return charge;
    }
}
