package id.payu.integration.adapter.persistence.entity;

import id.payu.integration.domain.model.MessageDirection;
import id.payu.integration.domain.model.MessageStatus;
import id.payu.integration.domain.model.MessageType;
import id.payu.security.annotation.Sensitive;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA entity for IntegrationMessage.
 */
@Entity
@Table(name = "integration_messages", indexes = {
    @Index(name = "idx_intmsg_status", columnList = "status"),
    @Index(name = "idx_intmsg_type", columnList = "type"),
    @Index(name = "idx_intmsg_created", columnList = "created_at"),
    @Index(name = "idx_intmsg_correlation", columnList = "correlation_id"),
    @Index(name = "idx_intmsg_business_ref", columnList = "business_reference")
})
// BUG-ARCH-005 FIX: Replaced @Data with @Getter @Setter to avoid Lombok-generated equals/hashCode on JPA entities
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationMessageEntity {

    @Id
    @Column(name = "message_id", length = 36)
    private String messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private MessageType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private MessageDirection direction;

    @Column(name = "source_system", length = 100)
    private String sourceSystem;

    @Column(name = "target_system", length = 100)
    private String targetSystem;

    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    @Sensitive
    @Column(name = "business_reference", length = 100)
    private String businessReference;

    @Sensitive(value = Sensitive.SensitivityLevel.HIGH)
    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Sensitive(value = Sensitive.SensitivityLevel.HIGH)
    @Column(name = "transformed_payload", columnDefinition = "TEXT")
    private String transformedPayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MessageStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "max_retries")
    private Integer maxRetries;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    @Version
    @Column(name = "version")
    private Long version;
}
