package id.payu.security.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.security.config.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import id.payu.outbox.service.OutboxService;

import java.time.Instant;
import java.util.UUID;
import java.util.Map;

/**
 * Publisher for audit events.
 * Sends audit events to Kafka for storage and analysis.
 * Bean creation managed by SecurityAutoConfiguration — do NOT add @Component.
 */
@Slf4j
public class AuditLogPublisher {

    private final SecurityProperties properties;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxService outboxService;

    public AuditLogPublisher(SecurityProperties properties,
                             KafkaTemplate<String, String> kafkaTemplate,
                             ObjectMapper objectMapper) {
        this(properties, kafkaTemplate, objectMapper, null);
    }

    public AuditLogPublisher(SecurityProperties properties,
                             KafkaTemplate<String, String> kafkaTemplate,
                             ObjectMapper objectMapper,
                             OutboxService outboxService) {
        this.properties = properties;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.outboxService = outboxService;
    }

    @Async
    public void publish(AuditEvent event) {
        // AUDIT-049 fail-fast: outbox is REQUIRED for audit log publishing.
        // Audit logs are compliance-critical (OJK/PCI-DSS) — direct Kafka bypass
        // is forbidden by AGENTS.md rule #4.
        if (outboxService == null) {
            throw new IllegalStateException(
                "AuditLogPublisher requires OutboxService (AGENTS.md rule #4: outbox-only). "
                    + "Audit log bypass via direct Kafka is forbidden — audit events are "
                    + "compliance-critical and must survive broker outages via outbox relay.");
        }

        try {
            if (!properties.getAudit().isEnabled()) {
                return;
            }

            // Check if operation should be audited
            boolean shouldAudit = properties.getAudit().getOperations().stream()
                    .anyMatch(op -> op.equalsIgnoreCase(event.getEventType()));

            if (!shouldAudit) {
                return;
            }

            // Set event ID and timestamp if not set
            if (event.getEventId() == null) {
                event.setEventId(UUID.randomUUID().toString());
            }
            if (event.getTimestamp() == null) {
                event.setTimestamp(Instant.now());
            }

            // Standardize topic name to: payu.security.audit-log.v1
            String topic = "payu.security.audit-log.v1";

            // AUDIT-049 fix: outbox-only path, KafkaTemplate fallback removed.
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(event, Map.class);

            outboxService.createEvent(
                "AuditLog",
                event.getEventId(),
                event.getEventType(),
                payload,
                null,
                topic
            );
            log.debug("Created outbox event for audit: {}", event.getEventId());

        } catch (Exception e) {
            log.error("Failed to publish/serialize audit event", e);
        }
    }

    /**
     * Publish audit event with masked sensitive data
     */
    @Async
    public void publishSafe(AuditEvent event) {
        // Mask sensitive data in context before publishing
        if (event.getContext() != null) {
            event.getContext().entrySet().forEach(entry -> {
                if (entry.getValue() instanceof String) {
                    String value = (String) entry.getValue();
                    // Mask common sensitive fields
                    if (entry.getKey().toLowerCase().contains("password") ||
                            entry.getKey().toLowerCase().contains("secret") ||
                            entry.getKey().toLowerCase().contains("token")) {
                        entry.setValue("****");
                    } else if (value.matches("\\d{10,}")) {
                        // Mask long numbers (account numbers, cards)
                        entry.setValue(maskAccountNumber(value));
                    } else if (value.contains("@")) {
                        // Mask emails
                        entry.setValue(maskEmail(value));
                    }
                }
            });
        }

        publish(event);
    }

    private String maskAccountNumber(String number) {
        if (number.length() <= 4) {
            return "****";
        }
        return number.substring(0, 4) + "******";
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "****" + email.substring(atIndex);
        }
        return email.charAt(0) + "****" + email.substring(atIndex);
    }
}
