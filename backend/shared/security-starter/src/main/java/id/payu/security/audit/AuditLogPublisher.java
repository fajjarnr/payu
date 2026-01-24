package id.payu.security.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.security.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Publisher for audit events
 * Sends audit events to Kafka for storage and analysis
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogPublisher {

    private final SecurityProperties properties;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Async("auditExecutor")
    public void publish(AuditEvent event) {
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

            // Serialize to JSON
            String json = objectMapper.writeValueAsString(event);

            // Send to Kafka
            kafkaTemplate.send("audit-logs", event.getEventId(), json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish audit event: {}", event.getEventId(), ex);
                        } else {
                            log.debug("Published audit event: {}", event.getEventId());
                        }
                    });

        } catch (Exception e) {
            log.error("Failed to serialize audit event", e);
        }
    }

    /**
     * Publish audit event with masked sensitive data
     */
    @Async("auditExecutor")
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
