package id.payu.billing.adapter.messaging;

import id.payu.billing.domain.event.SubscriptionEvent;
import id.payu.billing.adapter.persistence.entity.SubscriptionEntity;
import id.payu.billing.adapter.persistence.entity.SubscriptionChargeEntity;
import id.payu.billing.domain.port.out.SubscriptionEventPort;
import id.payu.events.cloudevents.CloudEventEnvelope;
import id.payu.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka adapter for publishing subscription lifecycle events.
 * Publishes CloudEvent envelopes to the payu.billing.subscription-event.v1 topic via Outbox.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventAdapter implements SubscriptionEventPort {

    private final OutboxService outboxService;
    private static final String TOPIC = "payu.billing.subscription-event.v1";

    @Override
    public void publishSubscriptionCreated(SubscriptionEntity subscription) {
        CloudEventEnvelope<SubscriptionEvent.SubscriptionCreatedPayload> event =
                SubscriptionEvent.createSubscriptionCreatedEvent(
                        subscription.getId(), subscription.getPartnerId(),
                        subscription.getAccountId(), subscription.getPlanId(),
                        subscription.getExternalReferenceId(),
                        subscription.getStatus().name(),
                        subscription.getCurrentPrice(), subscription.getCurrency(),
                        subscription.getTrialEndAt(), subscription.getCurrentPeriodStart(),
                        subscription.getCurrentPeriodEnd(), subscription.getNextBillingAt(),
                        subscription.getCreatedAt());

        outboxService.createEventFromObject(
                "Subscription",
                subscription.getId().toString(),
                SubscriptionEvent.SUBSCRIPTION_CREATED,
                event,
                Map.of("X-Partner-Id", subscription.getPartnerId()),
                TOPIC
        );
        log.info("Created outbox event for subscription.created: {}", subscription.getId());
    }

    @Override
    public void publishChargeSucceeded(SubscriptionEntity subscription, SubscriptionChargeEntity charge) {
        CloudEventEnvelope<SubscriptionEvent.ChargePayload> event =
                SubscriptionEvent.createChargeSucceededEvent(
                        charge.getId(), charge.getSubscriptionId(),
                        charge.getAccountId(), subscription.getPartnerId(),
                        subscription.getPlanId(), subscription.getExternalReferenceId(),
                        charge.getAmount(), charge.getCurrency(), charge.getAttemptNumber(),
                        charge.getBillingPeriodStart(), charge.getBillingPeriodEnd(),
                        charge.getChargedAt());

        outboxService.createEventFromObject(
                "SubscriptionCharge",
                charge.getId().toString(),
                SubscriptionEvent.CHARGE_SUCCEEDED,
                event,
                Map.of(
                        "X-Partner-Id", subscription.getPartnerId(),
                        "X-SubscriptionEntity-Id", subscription.getId().toString()
                ),
                TOPIC
        );
        log.info("Created outbox event for charge.succeeded: {}", charge.getId());
    }

    @Override
    public void publishChargeFailed(SubscriptionEntity subscription, SubscriptionChargeEntity charge) {
        CloudEventEnvelope<SubscriptionEvent.ChargePayload> event =
                SubscriptionEvent.createChargeFailedEvent(
                        charge.getId(), charge.getSubscriptionId(),
                        charge.getAccountId(), subscription.getPartnerId(),
                        subscription.getPlanId(), subscription.getExternalReferenceId(),
                        charge.getAmount(), charge.getCurrency(), charge.getAttemptNumber(),
                        charge.getBillingPeriodStart(), charge.getBillingPeriodEnd(),
                        charge.getChargedAt(), charge.getFailureReason());

        outboxService.createEventFromObject(
                "SubscriptionCharge",
                charge.getId().toString(),
                SubscriptionEvent.CHARGE_FAILED,
                event,
                Map.of(
                        "X-Partner-Id", subscription.getPartnerId(),
                        "X-SubscriptionEntity-Id", subscription.getId().toString()
                ),
                TOPIC
        );
        log.info("Created outbox event for charge.failed: {}", charge.getId());
    }
}
