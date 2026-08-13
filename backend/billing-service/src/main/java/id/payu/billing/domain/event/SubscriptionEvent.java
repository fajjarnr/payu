package id.payu.billing.domain.event;

import id.payu.events.cloudevents.CloudEventEnvelope;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain event for subscription lifecycle notifications.
 * Published to Kafka via outbox-starter and consumed by WebhookDispatcherService in partner-service.
 * <p>
 * ARCH-008: Factory methods accept primitive values instead of adapter persistence entities.
 * This keeps the domain layer independent of infrastructure concerns (hexagonal architecture).
 */
public class SubscriptionEvent {

    public static final String TOPIC = "subscription.events";
    public static final String SUBSCRIPTION_CREATED = "subscription.created";
    public static final String CHARGE_SUCCEEDED = "charge.succeeded";
    public static final String CHARGE_FAILED = "charge.failed";
    public static final String SUBSCRIPTION_DUE = "subscription.due";

    @Data
    @Builder
    public static class DuePayload {
        private String subscriptionId;
        private String partnerId;
        private String accountId;
        private Instant dueAt;
    }

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

    public static CloudEventEnvelope<SubscriptionCreatedPayload> createSubscriptionCreatedEvent(
            UUID subscriptionId, String partnerId, String accountId, UUID planId,
            String externalReferenceId, String status, BigDecimal currentPrice, String currency,
            LocalDateTime trialEndAt, LocalDateTime currentPeriodStart, LocalDateTime currentPeriodEnd,
            LocalDateTime nextBillingAt, LocalDateTime createdAt) {

        SubscriptionCreatedPayload payload = SubscriptionCreatedPayload.builder()
                .subscriptionId(subscriptionId.toString())
                .partnerId(partnerId)
                .accountId(accountId)
                .planId(planId.toString())
                .externalReferenceId(externalReferenceId)
                .status(status)
                .currentPrice(currentPrice)
                .currency(currency)
                .trialEndAt(toInstant(trialEndAt))
                .currentPeriodStart(toInstant(currentPeriodStart))
                .currentPeriodEnd(toInstant(currentPeriodEnd))
                .nextBillingAt(toInstant(nextBillingAt))
                .createdAt(toInstant(createdAt))
                .build();

        return CloudEventEnvelope.<SubscriptionCreatedPayload>builder()
                .id(UUID.randomUUID())
                .source(URI.create("/billing-service/subscriptions"))
                .type(SUBSCRIPTION_CREATED)
                .subject(subscriptionId.toString())
                .time(OffsetDateTime.now())
                .data(payload)
                .payuCorrelationId(UUID.randomUUID().toString())
                .build();
    }

    public static CloudEventEnvelope<ChargePayload> createChargeSucceededEvent(
            UUID chargeId, UUID subscriptionId, String accountId,
            String partnerId, UUID planId, String externalReferenceId,
            BigDecimal amount, String currency, int attemptNumber,
            LocalDateTime billingPeriodStart, LocalDateTime billingPeriodEnd,
            LocalDateTime chargedAt) {

        ChargePayload payload = ChargePayload.builder()
                .chargeId(chargeId.toString())
                .subscriptionId(subscriptionId.toString())
                .partnerId(partnerId)
                .accountId(accountId)
                .planId(planId.toString())
                .externalReferenceId(externalReferenceId)
                .amount(amount)
                .currency(currency)
                .status("SUCCEEDED")
                .attemptNumber(attemptNumber)
                .billingPeriodStart(toInstant(billingPeriodStart))
                .billingPeriodEnd(toInstant(billingPeriodEnd))
                .chargedAt(toInstant(chargedAt))
                .build();

        return CloudEventEnvelope.<ChargePayload>builder()
                .id(UUID.randomUUID())
                .source(URI.create("/billing-service/charges"))
                .type(CHARGE_SUCCEEDED)
                .subject(chargeId.toString())
                .time(OffsetDateTime.now())
                .data(payload)
                .payuCorrelationId(UUID.randomUUID().toString())
                .build();
    }

    public static CloudEventEnvelope<ChargePayload> createChargeFailedEvent(
            UUID chargeId, UUID subscriptionId, String accountId,
            String partnerId, UUID planId, String externalReferenceId,
            BigDecimal amount, String currency, int attemptNumber,
            LocalDateTime billingPeriodStart, LocalDateTime billingPeriodEnd,
            LocalDateTime chargedAt, String failureReason) {

        ChargePayload payload = ChargePayload.builder()
                .chargeId(chargeId.toString())
                .subscriptionId(subscriptionId.toString())
                .partnerId(partnerId)
                .accountId(accountId)
                .planId(planId.toString())
                .externalReferenceId(externalReferenceId)
                .amount(amount)
                .currency(currency)
                .status("FAILED")
                .attemptNumber(attemptNumber)
                .billingPeriodStart(toInstant(billingPeriodStart))
                .billingPeriodEnd(toInstant(billingPeriodEnd))
                .chargedAt(toInstant(chargedAt))
                .failureReason(failureReason)
                .build();

        return CloudEventEnvelope.<ChargePayload>builder()
                .id(UUID.randomUUID())
                .source(URI.create("/billing-service/charges"))
                .type(CHARGE_FAILED)
                .subject(chargeId.toString())
                .time(OffsetDateTime.now())
                .data(payload)
                .payuCorrelationId(UUID.randomUUID().toString())
                .build();
    }

    /**
     * ARCH-BILL-001: delayed-trigger replacement for the Artemis scheduled
     * queue. The consumer (billing itself) re-checks due-ness before charging.
     */
    public static CloudEventEnvelope<DuePayload> createSubscriptionDueEvent(
            UUID subscriptionId, String partnerId, String accountId, LocalDateTime dueAt) {

        DuePayload payload = DuePayload.builder()
                .subscriptionId(subscriptionId.toString())
                .partnerId(partnerId)
                .accountId(accountId)
                .dueAt(toInstant(dueAt))
                .build();

        return CloudEventEnvelope.<DuePayload>builder()
                .id(UUID.randomUUID())
                .source(URI.create("/billing-service/subscriptions"))
                .type(SUBSCRIPTION_DUE)
                .subject(subscriptionId.toString())
                .time(OffsetDateTime.now())
                .data(payload)
                .payuCorrelationId(UUID.randomUUID().toString())
                .build();
    }

    private static Instant toInstant(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
    }
}
