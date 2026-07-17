package id.payu.billing.adapter.messaging;

import id.payu.billing.domain.event.SubscriptionEvent;
import id.payu.billing.domain.model.Subscription;
import id.payu.billing.domain.model.SubscriptionCharge;
import id.payu.billing.domain.model.SubscriptionStatus;
import id.payu.billing.domain.model.ChargeStatus;
import id.payu.events.cloudevents.CloudEventEnvelope;
import id.payu.outbox.service.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionEventAdapter Unit Tests")
class SubscriptionEventAdapterTest {

    @Mock
    private OutboxService outboxService;

    private SubscriptionEventAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SubscriptionEventAdapter(outboxService);
    }

    @Test
    @DisplayName("should publish subscription.created event to correct topic via Outbox")
    void shouldPublishSubscriptionCreatedEvent() {
        // Given
        Subscription subscription = createSampleSubscription();

        // When
        adapter.publishSubscriptionCreated(subscription);

        // Then
        ArgumentCaptor<CloudEventEnvelope> eventCaptor = ArgumentCaptor.forClass(CloudEventEnvelope.class);
        ArgumentCaptor<Map> headersCaptor = ArgumentCaptor.forClass(Map.class);

        verify(outboxService).createEventFromObject(
                eq("Subscription"),
                eq(subscription.getId().toString()),
                eq(SubscriptionEvent.SUBSCRIPTION_CREATED),
                eventCaptor.capture(),
                headersCaptor.capture(),
                eq("payu.billing.subscription-event.v1")
        );

        Map<String, Object> headers = headersCaptor.getValue();
        assertEquals(subscription.getPartnerId(), headers.get("X-Partner-Id"));

        CloudEventEnvelope payload = eventCaptor.getValue();
        assertNotNull(payload);
        assertEquals(SubscriptionEvent.SUBSCRIPTION_CREATED, payload.getType());
    }

    @Test
    @DisplayName("should publish charge.succeeded event to correct topic via Outbox")
    void shouldPublishChargeSucceededEvent() {
        // Given
        Subscription subscription = createSampleSubscription();
        SubscriptionCharge charge = createSampleCharge(subscription.getId(), true);

        // When
        adapter.publishChargeSucceeded(subscription, charge);

        // Then
        ArgumentCaptor<CloudEventEnvelope> eventCaptor = ArgumentCaptor.forClass(CloudEventEnvelope.class);
        ArgumentCaptor<Map> headersCaptor = ArgumentCaptor.forClass(Map.class);

        verify(outboxService).createEventFromObject(
                eq("SubscriptionCharge"),
                eq(charge.getId().toString()),
                eq(SubscriptionEvent.CHARGE_SUCCEEDED),
                eventCaptor.capture(),
                headersCaptor.capture(),
                eq("payu.billing.subscription-event.v1")
        );

        Map<String, Object> headers = headersCaptor.getValue();
        assertEquals(subscription.getPartnerId(), headers.get("X-Partner-Id"));
        assertEquals(subscription.getId().toString(), headers.get("X-SubscriptionEntity-Id"));

        CloudEventEnvelope payload = eventCaptor.getValue();
        assertNotNull(payload);
        assertEquals(SubscriptionEvent.CHARGE_SUCCEEDED, payload.getType());
    }

    @Test
    @DisplayName("should publish charge.failed event to correct topic via Outbox")
    void shouldPublishChargeFailedEvent() {
        // Given
        Subscription subscription = createSampleSubscription();
        SubscriptionCharge charge = createSampleCharge(subscription.getId(), false);
        charge.markFailed("Insufficient balance");

        // When
        adapter.publishChargeFailed(subscription, charge);

        // Then
        ArgumentCaptor<CloudEventEnvelope> eventCaptor = ArgumentCaptor.forClass(CloudEventEnvelope.class);
        ArgumentCaptor<Map> headersCaptor = ArgumentCaptor.forClass(Map.class);

        verify(outboxService).createEventFromObject(
                eq("SubscriptionCharge"),
                eq(charge.getId().toString()),
                eq(SubscriptionEvent.CHARGE_FAILED),
                eventCaptor.capture(),
                headersCaptor.capture(),
                eq("payu.billing.subscription-event.v1")
        );

        Map<String, Object> headers = headersCaptor.getValue();
        assertEquals(subscription.getPartnerId(), headers.get("X-Partner-Id"));
        assertEquals(subscription.getId().toString(), headers.get("X-SubscriptionEntity-Id"));

        CloudEventEnvelope payload = eventCaptor.getValue();
        assertNotNull(payload);
        assertEquals(SubscriptionEvent.CHARGE_FAILED, payload.getType());
    }

    // Helper methods

    private Subscription createSampleSubscription() {
        Subscription sub = new Subscription();
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

    private SubscriptionCharge createSampleCharge(UUID subscriptionId, boolean succeeded) {
        SubscriptionCharge charge = new SubscriptionCharge();
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
