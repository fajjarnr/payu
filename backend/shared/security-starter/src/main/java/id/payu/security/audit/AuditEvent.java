package id.payu.security.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Audit event for sensitive operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    /**
     * Unique event ID
     */
    private String eventId;

    /**
     * Event type (CREATE, UPDATE, DELETE, etc.)
     */
    private String eventType;

    /**
     * Operation performed
     */
    private String operation;

    /**
     * User who performed the operation
     */
    private String userId;

    /**
     * Session ID
     */
    private String sessionId;

    /**
     * Target entity type
     */
    private String entityType;

    /**
     * Target entity ID
     */
    private String entityId;

    /**
     * IP address
     */
    private String ipAddress;

    /**
     * User agent
     */
    private String userAgent;

    /**
     * Additional context data
     */
    private Map<String, Object> context;

    /**
     * Timestamp
     */
    private Instant timestamp;

    /**
     * Success status
     */
    private boolean success;

    /**
     * Error message (if failed)
     */
    private String errorMessage;
}
