package id.payu.billing.adapter.messaging;

import id.payu.billing.domain.event.SubscriptionEvent;
import id.payu.billing.domain.model.Subscription;
import id.payu.billing.domain.model.SubscriptionCharge;
import id.payu.billing.domain.port.out.SubscriptionEventPort;
import id.payu.events.cloudevents.CloudEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Kafka adapter for publishing subscription lifecycle events.
 * Publishes CloudEvent envelopes to the subscription.events topic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventAdapter implements SubscriptionEventPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishSubscriptionCreated(Subscription subscription) {
        CloudEventEnvelope<SubscriptionEvent.SubscriptionCreatedPayload> event =
                SubscriptionEvent.createSubscriptionCreatedEvent(subscription);

        var message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, SubscriptionEvent.TOPIC)
                .setHeader(KafkaHeaders.KEY, subscription.getId().toString())
                .setHeader("X-Event-Type", SubscriptionEvent.SUBSCRIPTION_CREATED)
                .setHeader("X-Partner-Id", subscription.getPartnerId())
                .build();

        kafkaTemplate.send(message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish subscription.created event: subscriptionId={}, error={}",
                                subscription.getId(), ex.getMessage());
                    } else {
                        log.info("Published subscription.created event: subscriptionId={}, partnerId={}, offset={}",
                                subscription.getId(), subscription.getPartnerId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    @Override
    public void publishChargeSucceeded(Subscription subscription, SubscriptionCharge charge) {
        CloudEventEnvelope<SubscriptionEvent.ChargePayload> event =
                SubscriptionEvent.createChargeSucceededEvent(subscription, charge);

        var message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, SubscriptionEvent.TOPIC)
                .setHeader(KafkaHeaders.KEY, charge.getId().toString())
                .setHeader("X-Event-Type", SubscriptionEvent.CHARGE_SUCCEEDED)
                .setHeader("X-Partner-Id", subscription.getPartnerId())
                .setHeader("X-Subscription-Id", subscription.getId().toString())
                .build();

        kafkaTemplate.send(message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish charge.succeeded event: chargeId={}, error={}",
                                charge.getId(), ex.getMessage());
                    } else {
                        log.info("Published charge.succeeded event: chargeId={}, subscriptionId={}, amount={} {}",
                                charge.getId(), subscription.getId(),
                                charge.getAmount(), charge.getCurrency());
                    }
                });
    }

    @Override
    public void publishChargeFailed(Subscription subscription, SubscriptionCharge charge) {
        CloudEventEnvelope<SubscriptionEvent.ChargePayload> event =
                SubscriptionEvent.createChargeFailedEvent(subscription, charge);

        var message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, SubscriptionEvent.TOPIC)
                .setHeader(KafkaHeaders.KEY, charge.getId().toString())
                .setHeader("X-Event-Type", SubscriptionEvent.CHARGE_FAILED)
                .setHeader("X-Partner-Id", subscription.getPartnerId())
                .setHeader("X-Subscription-Id", subscription.getId().toString())
                .build();

        kafkaTemplate.send(message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish charge.failed event: chargeId={}, error={}",
                                charge.getId(), ex.getMessage());
                    } else {
                        log.warn("Published charge.failed event: chargeId={}, subscriptionId={}, attempt={}/{}",
                                charge.getId(), subscription.getId(),
                                charge.getAttemptNumber(), subscription.getDunningAttempts());
                    }
                });
    }
}
