package id.payu.billing.domain.event;

import id.payu.billing.adapter.persistence.entity.SubscriptionEntity;
import id.payu.billing.adapter.persistence.entity.SubscriptionChargeEntity;
import id.payu.billing.domain.model.SubscriptionStatus;
import id.payu.billing.domain.model.ChargeStatus;
import id.payu.events.cloudevents.CloudEventEnvelope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SubscriptionEvent Domain Tests")
class SubscriptionEventTest {

    @Test
    @DisplayName("should create subscription.created event with correct type")
    void shouldCreateSubscriptionCreatedEvent() {
        SubscriptionEntity subscription = createSampleSubscription();

        CloudEventEnvelope<SubscriptionEvent.SubscriptionCreatedPayload> event =
                SubscriptionEvent.createSubscriptionCreatedEvent(subscription);

        assertNotNull(event);
        assertNotNull(event.getId());
        assertEquals(SubscriptionEvent.SUBSCRIPTION_CREATED, event.getType());
        assertEquals("/billing-service/subscriptions", event.getSource().toString());
        assertEquals(subscription.getId().toString(), event.getSubject());
        assertNotNull(event.getTime());
        assertNotNull(event.getPayuCorrelationId());

        SubscriptionEvent.SubscriptionCreatedPayload payload = event.getData();
        assertNotNull(payload);
        assertEquals(subscription.getId().toString(), payload.getSubscriptionId());
        assertEquals(subscription.getPartnerId(), payload.getPartnerId());
        assertEquals(subscription.getAccountId(), payload.getAccountId());
        assertEquals(subscription.getPlanId().toString(), payload.getPlanId());
        assertEquals(subscription.getExternalReferenceId(), payload.getExternalReferenceId());
        assertEquals(subscription.getStatus().name(), payload.getStatus());
        assertEquals(subscription.getCurrentPrice(), payload.getCurrentPrice());
        assertEquals(subscription.getCurrency(), payload.getCurrency());
    }

    @Test
    @DisplayName("should create charge.succeeded event with correct type")
    void shouldCreateChargeSucceededEvent() {
        SubscriptionEntity subscription = createSampleSubscription();
        SubscriptionChargeEntity charge = createSampleCharge(subscription.getId(), true);

        CloudEventEnvelope<SubscriptionEvent.ChargePayload> event =
                SubscriptionEvent.createChargeSucceededEvent(subscription, charge);

        assertNotNull(event);
        assertNotNull(event.getId());
        assertEquals(SubscriptionEvent.CHARGE_SUCCEEDED, event.getType());
        assertEquals("/billing-service/charges", event.getSource().toString());
        assertEquals(charge.getId().toString(), event.getSubject());
        assertNotNull(event.getTime());

        SubscriptionEvent.ChargePayload payload = event.getData();
        assertNotNull(payload);
        assertEquals(charge.getId().toString(), payload.getChargeId());
        assertEquals(subscription.getId().toString(), payload.getSubscriptionId());
        assertEquals(subscription.getPartnerId(), payload.getPartnerId());
        assertEquals(charge.getAccountId(), payload.getAccountId());
        assertEquals(charge.getAmount(), payload.getAmount());
        assertEquals(charge.getCurrency(), payload.getCurrency());
        assertEquals("SUCCEEDED", payload.getStatus());
        assertEquals(charge.getAttemptNumber(), payload.getAttemptNumber());
        assertNull(payload.getFailureReason());
    }

    @Test
    @DisplayName("should create charge.failed event with correct type and failure reason")
    void shouldCreateChargeFailedEvent() {
        SubscriptionEntity subscription = createSampleSubscription();
        SubscriptionChargeEntity charge = createSampleCharge(subscription.getId(), false);
        charge.markFailed("Insufficient balance");

        CloudEventEnvelope<SubscriptionEvent.ChargePayload> event =
                SubscriptionEvent.createChargeFailedEvent(subscription, charge);

        assertNotNull(event);
        assertNotNull(event.getId());
        assertEquals(SubscriptionEvent.CHARGE_FAILED, event.getType());
        assertEquals("/billing-service/charges", event.getSource().toString());
        assertEquals(charge.getId().toString(), event.getSubject());

        SubscriptionEvent.ChargePayload payload = event.getData();
        assertNotNull(payload);
        assertEquals(charge.getId().toString(), payload.getChargeId());
        assertEquals(subscription.getId().toString(), payload.getSubscriptionId());
        assertEquals(subscription.getPartnerId(), payload.getPartnerId());
        assertEquals(charge.getAmount(), payload.getAmount());
        assertEquals("FAILED", payload.getStatus());
        assertEquals("Insufficient balance", payload.getFailureReason());
    }

    @Test
    @DisplayName("should convert LocalDateTime to Instant correctly")
    void shouldConvertLocalDateTimeToInstant() {
        SubscriptionEntity subscription = createSampleSubscription();
        LocalDateTime now = LocalDateTime.now();
        subscription.setTrialEndAt(now);
        subscription.setCurrentPeriodStart(now);
        subscription.setCurrentPeriodEnd(now.plusMonths(1));
        subscription.setNextBillingAt(now.plusMonths(1));
        subscription.setCreatedAt(now);

        CloudEventEnvelope<SubscriptionEvent.SubscriptionCreatedPayload> event =
                SubscriptionEvent.createSubscriptionCreatedEvent(subscription);

        SubscriptionEvent.SubscriptionCreatedPayload payload = event.getData();
        assertNotNull(payload.getTrialEndAt());
        assertNotNull(payload.getCurrentPeriodStart());
        assertNotNull(payload.getCurrentPeriodEnd());
        assertNotNull(payload.getNextBillingAt());
        assertNotNull(payload.getCreatedAt());
    }

    @Test
    @DisplayName("should handle null dates gracefully")
    void shouldHandleNullDates() {
        SubscriptionEntity subscription = createSampleSubscription();
        subscription.setTrialEndAt(null);
        subscription.setCurrentPeriodStart(null);
        subscription.setCurrentPeriodEnd(null);
        subscription.setNextBillingAt(null);

        CloudEventEnvelope<SubscriptionEvent.SubscriptionCreatedPayload> event =
                SubscriptionEvent.createSubscriptionCreatedEvent(subscription);

        SubscriptionEvent.SubscriptionCreatedPayload payload = event.getData();
        assertNull(payload.getTrialEndAt());
        assertNull(payload.getCurrentPeriodStart());
        assertNull(payload.getCurrentPeriodEnd());
        assertNull(payload.getNextBillingAt());
    }

    // Helper methods

    private SubscriptionEntity createSampleSubscription() {
        SubscriptionEntity sub = new SubscriptionEntity();
        sub.setId(UUID.randomUUID());
        sub.setAccountId("acc-123456");
        sub.setPlanId(UUID.randomUUID());
        sub.setPartnerId("partner-nobar");
        sub.setStatus(SubscriptionStatus.ACTIVE);
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
            charge.setStatus(ChargeStatus.FAILED);
        }

        return charge;
    }
}
