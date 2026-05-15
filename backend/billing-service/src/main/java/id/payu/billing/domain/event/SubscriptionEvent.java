package id.payu.billing.domain.event;

import id.payu.billing.adapter.persistence.entity.SubscriptionEntity;
import id.payu.billing.adapter.persistence.entity.SubscriptionChargeEntity;
import id.payu.events.cloudevents.CloudEventEnvelope;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain event for subscription lifecycle notifications.
 * Published to Kafka and consumed by WebhookDispatcherService in partner-service.
 */
public class SubscriptionEvent {

    public static final String TOPIC = "subscription.events";

    // Event types
    public static final String SUBSCRIPTION_CREATED = "subscription.created";
    public static final String CHARGE_SUCCEEDED = "charge.succeeded";
    public static final String CHARGE_FAILED = "charge.failed";

    /**
     * Payload for subscription.created event.
     */
    @Data
    @Builder
    public static class SubscriptionCreatedPayload {
        private String subscriptionId;
        private String partnerId;
        private String accountId;
        private String planId;
        private String externalReferenceId;
        private String status;
        private BigDecimal currentPrice;
        private String currency;
        private Instant trialEndAt;
        private Instant currentPeriodStart;
        private Instant currentPeriodEnd;
        private Instant nextBillingAt;
        private Instant createdAt;
    }

    /**
     * Payload for charge.succeeded and charge.failed events.
     */
    @Data
    @Builder
    public static class ChargePayload {
        private String chargeId;
        private String subscriptionId;
        private String partnerId;
        private String accountId;
        private String planId;
        private String externalReferenceId;
        private BigDecimal amount;
        private String currency;
        private String status;
        private int attemptNumber;
        private Instant billingPeriodStart;
        private Instant billingPeriodEnd;
        private Instant chargedAt;
        private String failureReason;
    }

    /**
     * Create a CloudEvent envelope for subscription.created event.
     */
    public static CloudEventEnvelope<SubscriptionCreatedPayload> createSubscriptionCreatedEvent(
            SubscriptionEntity subscription) {

        SubscriptionCreatedPayload payload = SubscriptionCreatedPayload.builder()
                .subscriptionId(subscription.getId().toString())
                .partnerId(subscription.getPartnerId())
                .accountId(subscription.getAccountId())
                .planId(subscription.getPlanId().toString())
                .externalReferenceId(subscription.getExternalReferenceId())
                .status(subscription.getStatus().name())
                .currentPrice(subscription.getCurrentPrice())
                .currency(subscription.getCurrency())
                .trialEndAt(toInstant(subscription.getTrialEndAt()))
                .currentPeriodStart(toInstant(subscription.getCurrentPeriodStart()))
                .currentPeriodEnd(toInstant(subscription.getCurrentPeriodEnd()))
                .nextBillingAt(toInstant(subscription.getNextBillingAt()))
                .createdAt(toInstant(subscription.getCreatedAt()))
                .build();

        return CloudEventEnvelope.<SubscriptionCreatedPayload>builder()
                .id(UUID.randomUUID())
                .source(URI.create("/billing-service/subscriptions"))
                .type(SUBSCRIPTION_CREATED)
                .subject(subscription.getId().toString())
                .time(OffsetDateTime.now())
                .data(payload)
                .payuCorrelationId(UUID.randomUUID().toString())
                .build();
    }

    /**
     * Create a CloudEvent envelope for charge.succeeded event.
     */
    public static CloudEventEnvelope<ChargePayload> createChargeSucceededEvent(
            SubscriptionEntity subscription, SubscriptionChargeEntity charge) {

        ChargePayload payload = ChargePayload.builder()
                .chargeId(charge.getId().toString())
                .subscriptionId(charge.getSubscriptionId().toString())
                .partnerId(subscription.getPartnerId())
                .accountId(charge.getAccountId())
                .planId(subscription.getPlanId().toString())
                .externalReferenceId(subscription.getExternalReferenceId())
                .amount(charge.getAmount())
                .currency(charge.getCurrency())
                .status("SUCCEEDED")
                .attemptNumber(charge.getAttemptNumber())
                .billingPeriodStart(toInstant(charge.getBillingPeriodStart()))
                .billingPeriodEnd(toInstant(charge.getBillingPeriodEnd()))
                .chargedAt(toInstant(charge.getChargedAt()))
                .build();

        return CloudEventEnvelope.<ChargePayload>builder()
                .id(UUID.randomUUID())
                .source(URI.create("/billing-service/charges"))
                .type(CHARGE_SUCCEEDED)
                .subject(charge.getId().toString())
                .time(OffsetDateTime.now())
                .data(payload)
                .payuCorrelationId(UUID.randomUUID().toString())
                .build();
    }

    /**
     * Create a CloudEvent envelope for charge.failed event.
     */
    public static CloudEventEnvelope<ChargePayload> createChargeFailedEvent(
            SubscriptionEntity subscription, SubscriptionChargeEntity charge) {

        ChargePayload payload = ChargePayload.builder()
                .chargeId(charge.getId().toString())
                .subscriptionId(charge.getSubscriptionId().toString())
                .partnerId(subscription.getPartnerId())
                .accountId(charge.getAccountId())
                .planId(subscription.getPlanId().toString())
                .externalReferenceId(subscription.getExternalReferenceId())
                .amount(charge.getAmount())
                .currency(charge.getCurrency())
                .status("FAILED")
                .attemptNumber(charge.getAttemptNumber())
                .billingPeriodStart(toInstant(charge.getBillingPeriodStart()))
                .billingPeriodEnd(toInstant(charge.getBillingPeriodEnd()))
                .chargedAt(toInstant(charge.getChargedAt()))
                .failureReason(charge.getFailureReason())
                .build();

        return CloudEventEnvelope.<ChargePayload>builder()
                .id(UUID.randomUUID())
                .source(URI.create("/billing-service/charges"))
                .type(CHARGE_FAILED)
                .subject(charge.getId().toString())
                .time(OffsetDateTime.now())
                .data(payload)
                .payuCorrelationId(UUID.randomUUID().toString())
                .build();
    }

    private static Instant toInstant(java.time.LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
    }
}
