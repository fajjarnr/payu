package id.payu.cms.adapter.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.cms.domain.entity.Content;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Event publisher for content-related events
 * Implements messaging adapter in hexagonal architecture
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String contentPublishedTopic;
    private final String contentUpdatedTopic;
    private final String contentArchivedTopic;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Publish content published event
     */
    public void publishContentPublished(Content content) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventId", java.util.UUID.randomUUID().toString());
            event.put("eventType", "CONTENT_PUBLISHED");
            event.put("contentId", content.getId().toString());
            event.put("contentType", content.getContentType());
            event.put("title", content.getTitle());
            event.put("status", content.getStatus().name());
            event.put("startDate", content.getStartDate());
            event.put("endDate", content.getEndDate());
            event.put("priority", content.getPriority());
            event.put("targetingRules", content.getTargetingRules());
            event.put("publishedAt", LocalDateTime.now().toString());
            event.put("publishedBy", content.getUpdatedBy());

            String jsonEvent = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(contentPublishedTopic, content.getId().toString(), jsonEvent);

            log.info("Published content published event: {}", content.getId());
        } catch (Exception e) {
            log.error("Failed to publish content published event: {}", content.getId(), e);
        }
    }

    /**
     * Publish content updated event
     */
    public void publishContentUpdated(Content content) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventId", java.util.UUID.randomUUID().toString());
            event.put("eventType", "CONTENT_UPDATED");
            event.put("contentId", content.getId().toString());
            event.put("contentType", content.getContentType());
            event.put("title", content.getTitle());
            event.put("status", content.getStatus().name());
            event.put("version", content.getVersion());
            event.put("updatedAt", content.getUpdatedAt().toString());
            event.put("updatedBy", content.getUpdatedBy());

            String jsonEvent = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(contentUpdatedTopic, content.getId().toString(), jsonEvent);

            log.info("Published content updated event: {}", content.getId());
        } catch (Exception e) {
            log.error("Failed to publish content updated event: {}", content.getId(), e);
        }
    }

    /**
     * Publish content archived event
     */
    public void publishContentArchived(Content content) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventId", java.util.UUID.randomUUID().toString());
            event.put("eventType", "CONTENT_ARCHIVED");
            event.put("contentId", content.getId().toString());
            event.put("contentType", content.getContentType());
            event.put("title", content.getTitle());
            event.put("archivedAt", LocalDateTime.now().toString());

            String jsonEvent = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(contentArchivedTopic, content.getId().toString(), jsonEvent);

            log.info("Published content archived event: {}", content.getId());
        } catch (Exception e) {
            log.error("Failed to publish content archived event: {}", content.getId(), e);
        }
    }
}
