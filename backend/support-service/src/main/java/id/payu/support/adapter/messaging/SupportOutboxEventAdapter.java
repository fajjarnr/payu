package id.payu.support.adapter.messaging;

import id.payu.outbox.repository.OutboxRepository;
import id.payu.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Outbox adapter for support ticket events — replaces log-only ponytail.
 * Topic: payu.support.ticket-created.v1 (CloudEvents 1.0.2 via outbox-starter)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SupportOutboxEventAdapter {

    private final OutboxService outboxService;
    private final OutboxRepository outboxRepository;

    public void publishTicketCreated(UUID ticketId, String userId, String subject) {
        if (outboxRepository.findFirstByAggregateTypeAndAggregateIdAndEventType(
                "SupportTicketEntity", ticketId.toString(), "TicketCreated").isPresent()) {
            return;
        }
        Map<String, Object> payload = Map.of(
                "ticketId", ticketId.toString(),
                "userId", userId,
                "subject", subject != null ? subject : ""
        );
        outboxService.createEvent(
                "SupportTicketEntity",
                ticketId.toString(),
                "TicketCreated",
                payload
        );
        log.info("Published ticket-created event: id={} user={}", ticketId, userId);
    }
}
