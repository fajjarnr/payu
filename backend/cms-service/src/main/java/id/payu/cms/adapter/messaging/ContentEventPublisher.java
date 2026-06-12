package id.payu.cms.adapter.messaging;

import id.payu.cms.adapter.persistence.entity.ContentEntity;
import id.payu.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Event publisher for content-related events.
 * Implements messaging adapter in hexagonal architecture.
 * <p>
 * MSG-010: Migrated from KafkaTemplate to OutboxService
 * for transactional atomicity between content changes and event publishing.
 *
 * @author PayU Digital Banking Platform
 * @since 1.8.8
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentEventPublisher {

    private final OutboxService outboxService;

    private static final String AGGREGATE_TYPE = "Content";
    private static final String TOPIC_PUBLISHED = "payu.cms.content-published.v1";
    private static final String TOPIC_UPDATED = "payu.cms.content-updated.v1";
    private static final String TOPIC_ARCHIVED = "payu.cms.content-archived.v1";

    /**
     * Publish content published event
     */
    public void publishContentPublished(ContentEntity content) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "CONTENT_PUBLISHED");
        event.put("contentId", content.getId().toString());
        event.put("contentType", content.getContentType());
        event.put("title", content.getTitle());
        event.put("status", content.getStatus().name());
        event.put("startDate", content.getStartDate() != null ? content.getStartDate().toString() : null);
        event.put("endDate", content.getEndDate() != null ? content.getEndDate().toString() : null);
        event.put("priority", content.getPriority());
        event.put("targetingRules", content.getTargetingRules());
        event.put("publishedAt", LocalDateTime.now().toString());
        event.put("publishedBy", content.getUpdatedBy());

        outboxService.createEvent(
                AGGREGATE_TYPE,
                content.getId().toString(),
                "ContentPublished",
                event,
                null,
                TOPIC_PUBLISHED
        );

        log.info("Created outbox event for content published: {}", content.getId());
    }

    /**
     * Publish content updated event
     */
    public void publishContentUpdated(ContentEntity content) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "CONTENT_UPDATED");
        event.put("contentId", content.getId().toString());
        event.put("contentType", content.getContentType());
        event.put("title", content.getTitle());
        event.put("status", content.getStatus().name());
        event.put("version", content.getVersion());
        event.put("updatedAt", content.getUpdatedAt() != null ? content.getUpdatedAt().toString() : null);
        event.put("updatedBy", content.getUpdatedBy());

        outboxService.createEvent(
                AGGREGATE_TYPE,
                content.getId().toString(),
                "ContentUpdated",
                event,
                null,
                TOPIC_UPDATED
        );

        log.info("Created outbox event for content updated: {}", content.getId());
    }

    /**
     * Publish content archived event
     */
    public void publishContentArchived(ContentEntity content) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "CONTENT_ARCHIVED");
        event.put("contentId", content.getId().toString());
        event.put("contentType", content.getContentType());
        event.put("title", content.getTitle());
        event.put("archivedAt", LocalDateTime.now().toString());

        outboxService.createEvent(
                AGGREGATE_TYPE,
                content.getId().toString(),
                "ContentArchived",
                event,
                null,
                TOPIC_ARCHIVED
        );

        log.info("Created outbox event for content archived: {}", content.getId());
    }
}
