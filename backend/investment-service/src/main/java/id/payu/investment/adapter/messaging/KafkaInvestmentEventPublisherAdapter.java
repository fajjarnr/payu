package id.payu.investment.adapter.messaging;

import id.payu.investment.domain.port.out.InvestmentEventPublisherPort;
import id.payu.investment.interfaces.dto.InvestmentEvent;
import id.payu.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Outbox-based adapter for publishing investment events.
 * <p>
 * MSG-011: Migrated from KafkaTemplate to OutboxService
 * for transactional atomicity between investment state changes and event publishing.
 *
 * @author PayU Digital Banking Platform
 * @since 1.8.8
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaInvestmentEventPublisherAdapter implements InvestmentEventPublisherPort {

    private final OutboxService outboxService;
    private static final String TOPIC_CREATED = "payu.investment.created.v1";
    private static final String TOPIC_COMPLETED = "payu.investment.completed.v1";
    private static final String TOPIC_FAILED = "payu.investment.failed.v1";
    private static final String AGGREGATE_TYPE = "Investment";

    @Override
    public void publishInvestmentCreated(InvestmentEvent event) {
        log.info("Creating outbox event for investment created: {}", event.id());
        outboxService.createEventFromObject(
                AGGREGATE_TYPE,
                event.id().toString(),
                "InvestmentCreated",
                event,
                null,
                TOPIC_CREATED
        );
    }

    @Override
    public void publishInvestmentCompleted(InvestmentEvent event) {
        log.info("Creating outbox event for investment completed: {}", event.id());
        outboxService.createEventFromObject(
                AGGREGATE_TYPE,
                event.id().toString(),
                "InvestmentCompleted",
                event,
                null,
                TOPIC_COMPLETED
        );
    }

    @Override
    public void publishInvestmentFailed(InvestmentEvent event) {
        log.info("Creating outbox event for investment failed: {}", event.id());
        outboxService.createEventFromObject(
                AGGREGATE_TYPE,
                event.id().toString(),
                "InvestmentFailed",
                event,
                null,
                TOPIC_FAILED
        );
    }
}
