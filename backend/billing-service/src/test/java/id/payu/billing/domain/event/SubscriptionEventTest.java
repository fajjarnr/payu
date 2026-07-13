package id.payu.billing.domain.event;

import id.payu.events.cloudevents.CloudEventEnvelope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SubscriptionEvent Domain Tests")
class SubscriptionEventTest {

    private final UUID subId = UUID.randomUUID();
    private final UUID planId = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.now();

    @Test
    @DisplayName("should create subscription.created event with correct type")
    void shouldCreateSubscriptionCreatedEvent() {
        CloudEventEnvelope<SubscriptionEvent.SubscriptionCreatedPayload> event =
                SubscriptionEvent.createSubscriptionCreatedEvent(
                        subId, "partner-nobar", "acc-123456", planId,
                        "ext-ref-001", "ACTIVE", new BigDecimal("99000"), "IDR",
                        now, now, now.plusMonths(1), now.plusMonths(1), now);

        assertNotNull(event);
        assertNotNull(event.getId());
        assertEquals(SubscriptionEvent.SUBSCRIPTION_CREATED, event.getType());
        assertEquals("/billing-service/subscriptions", event.getSource().toString());
        assertEquals(subId.toString(), event.getSubject());
        assertNotNull(event.getTime());
        assertNotNull(event.getPayuCorrelationId());

        SubscriptionEvent.SubscriptionCreatedPayload payload = event.getData();
        assertNotNull(payload);
        assertEquals(subId.toString(), payload.getSubscriptionId());
        assertEquals("partner-nobar", payload.getPartnerId());
        assertEquals("acc-123456", payload.getAccountId());
        assertEquals(planId.toString(), payload.getPlanId());
        assertEquals("ext-ref-001", payload.getExternalReferenceId());
        assertEquals("ACTIVE", payload.getStatus());
        assertEquals(new BigDecimal("99000"), payload.getCurrentPrice());
        assertEquals("IDR", payload.getCurrency());
    }

    @Test
    @DisplayName("should create charge.succeeded event with correct type")
    void shouldCreateChargeSucceededEvent() {
        UUID chargeId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        CloudEventEnvelope<SubscriptionEvent.ChargePayload> event =
                SubscriptionEvent.createChargeSucceededEvent(
                        chargeId, subscriptionId, "acc-123456",
                        "partner-nobar", planId, "ext-ref-001",
                        new BigDecimal("99000"), "IDR", 1,
                        now.minusMonths(1), now, now);

        assertNotNull(event);
        assertNotNull(event.getId());
        assertEquals(SubscriptionEvent.CHARGE_SUCCEEDED, event.getType());
        assertEquals("/billing-service/charges", event.getSource().toString());
        assertEquals(chargeId.toString(), event.getSubject());
        assertNotNull(event.getTime());

        SubscriptionEvent.ChargePayload payload = event.getData();
        assertNotNull(payload);
        assertEquals(chargeId.toString(), payload.getChargeId());
        assertEquals(subscriptionId.toString(), payload.getSubscriptionId());
        assertEquals("partner-nobar", payload.getPartnerId());
        assertEquals("acc-123456", payload.getAccountId());
        assertEquals(new BigDecimal("99000"), payload.getAmount());
        assertEquals("IDR", payload.getCurrency());
        assertEquals("SUCCEEDED", payload.getStatus());
        assertEquals(1, payload.getAttemptNumber());
        assertNull(payload.getFailureReason());
    }

    @Test
    @DisplayName("should create charge.failed event with correct type and failure reason")
    void shouldCreateChargeFailedEvent() {
        UUID chargeId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        CloudEventEnvelope<SubscriptionEvent.ChargePayload> event =
                SubscriptionEvent.createChargeFailedEvent(
                        chargeId, subscriptionId, "acc-123456",
                        "partner-nobar", planId, "ext-ref-001",
                        new BigDecimal("99000"), "IDR", 1,
                        now.minusMonths(1), now, now, "Insufficient balance");

        assertNotNull(event);
        assertNotNull(event.getId());
        assertEquals(SubscriptionEvent.CHARGE_FAILED, event.getType());
        assertEquals("/billing-service/charges", event.getSource().toString());
        assertEquals(chargeId.toString(), event.getSubject());

        SubscriptionEvent.ChargePayload payload = event.getData();
        assertNotNull(payload);
        assertEquals(chargeId.toString(), payload.getChargeId());
        assertEquals(subscriptionId.toString(), payload.getSubscriptionId());
        assertEquals("partner-nobar", payload.getPartnerId());
        assertEquals(new BigDecimal("99000"), payload.getAmount());
        assertEquals("FAILED", payload.getStatus());
        assertEquals("Insufficient balance", payload.getFailureReason());
    }

    @Test
    @DisplayName("should convert LocalDateTime to Instant correctly")
    void shouldConvertLocalDateTimeToInstant() {
        CloudEventEnvelope<SubscriptionEvent.SubscriptionCreatedPayload> event =
                SubscriptionEvent.createSubscriptionCreatedEvent(
                        subId, "partner-nobar", "acc-123456", planId,
                        "ext-ref-001", "ACTIVE", new BigDecimal("99000"), "IDR",
                        now, now, now.plusMonths(1), now.plusMonths(1), now);

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
        CloudEventEnvelope<SubscriptionEvent.SubscriptionCreatedPayload> event =
                SubscriptionEvent.createSubscriptionCreatedEvent(
                        subId, "partner-nobar", "acc-123456", planId,
                        "ext-ref-001", "ACTIVE", new BigDecimal("99000"), "IDR",
                        null, null, null, null, now);

        SubscriptionEvent.SubscriptionCreatedPayload payload = event.getData();
        assertNull(payload.getTrialEndAt());
        assertNull(payload.getCurrentPeriodStart());
        assertNull(payload.getCurrentPeriodEnd());
        assertNull(payload.getNextBillingAt());
    }
}
