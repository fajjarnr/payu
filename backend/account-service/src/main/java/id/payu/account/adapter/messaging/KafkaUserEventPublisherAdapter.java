package id.payu.account.adapter.messaging;

import id.payu.account.domain.port.out.UserEventPublisherPort;
import id.payu.account.dto.UserCreatedEvent;
import id.payu.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Outbox-based implementation of UserEventPublisherPort.
 * <p>
 * MSG-007: Migrated from direct KafkaTemplate.send() to OutboxService.createEvent()
 * for transactional atomicity between business data and event publishing.
 * Events are persisted to outbox_events table within the caller's transaction
 * and published to Kafka asynchronously by OutboxPublisher.
 *
 * @author PayU Digital Banking Platform
 * @since 1.8.8
 */
@Component
@RequiredArgsConstructor
public class KafkaUserEventPublisherAdapter implements UserEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaUserEventPublisherAdapter.class);

    private final OutboxService outboxService;

    private static final String AGGREGATE_TYPE = "User";
    private static final String TOPIC_USER_CREATED = "payu.account.user-created.v1";
    private static final String TOPIC_USER_UPDATED = "payu.account.user-updated.v1";
    private static final String TOPIC_KYC_COMPLETED = "payu.account.kyc-completed.v1";

    @Override
    public void publishUserCreated(UserCreatedEvent event) {
        log.info("Creating outbox event for UserCreated: userId={}", event.userId());
        outboxService.createEvent(
                AGGREGATE_TYPE,
                event.userId().toString(),
                "UserCreated",
                buildPayload(event),
                null,
                TOPIC_USER_CREATED
        );
    }

    @Override
    public void publishUserUpdated(UserCreatedEvent event) {
        log.info("Creating outbox event for UserUpdated: userId={}", event.userId());
        outboxService.createEvent(
                AGGREGATE_TYPE,
                event.userId().toString(),
                "UserUpdated",
                buildPayload(event),
                null,
                TOPIC_USER_UPDATED
        );
    }

    @Override
    public void publishKycCompleted(UserCreatedEvent event) {
        log.info("Creating outbox event for KycCompleted: userId={}", event.userId());
        outboxService.createEvent(
                AGGREGATE_TYPE,
                event.userId().toString(),
                "KycCompleted",
                buildPayload(event),
                null,
                TOPIC_KYC_COMPLETED
        );
    }

    private Map<String, Object> buildPayload(UserCreatedEvent event) {
        return Map.of(
                "userId", event.userId().toString(),
                "externalId", event.externalId() != null ? event.externalId() : "",
                "createdAt", event.createdAt() != null ? event.createdAt().toString() : ""
        );
    }
}
