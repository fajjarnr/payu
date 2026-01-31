package id.payu.saga.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base entity for saga instance persistence.
 * Stores the state of a running or completed saga orchestration.
 */
@Entity
@Table(name = "saga_instances", indexes = {
    @Index(name = "idx_saga_type", columnList = "sagaType"),
    @Index(name = "idx_saga_state", columnList = "currentState"),
    @Index(name = "idx_saga_started", columnList = "startedAt"),
    @Index(name = "idx_saga_completed", columnList = "completedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaInstance {

    @Id
    @Column(name = "saga_id", nullable = false, updatable = false, length = 36)
    private String sagaId;

    @Column(name = "saga_type", nullable = false, length = 100)
    private String sagaType;

    @Column(name = "current_state", nullable = false, length = 50)
    private String currentState;

    @Column(name = "previous_state", length = 50)
    private String previousState;

    /**
     * JSONB payload containing saga-specific data.
     * Uses PostgreSQL JSONB type for efficient querying.
     */
    @Type(JsonType.class)
    @Column(name = "payload", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> payload = new HashMap<>();

    /**
     * Stores the execution context for each step.
     * Key: step name, Value: step execution result/context
     */
    @Type(JsonType.class)
    @Column(name = "step_context", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> stepContext = new HashMap<>();

    /**
     * List of completed steps in order.
     * Used for compensation ordering (LIFO).
     */
    @Type(JsonType.class)
    @Column(name = "completed_steps", columnDefinition = "jsonb")
    @Builder.Default
    private java.util.List<String> completedSteps = new java.util.ArrayList<>();

    @Column(name = "started_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_updated_at", nullable = false)
    @UpdateTimestamp
    private Instant lastUpdatedAt;

    /**
     * Optimistic locking version for concurrency control.
     */
    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "error_step", length = 100)
    private String errorStep;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * Creates a new saga instance with generated UUID.
     */
    public static SagaInstance create(String sagaType, String initialState, Map<String, Object> payload) {
        return SagaInstance.builder()
                .sagaId(UUID.randomUUID().toString())
                .sagaType(sagaType)
                .currentState(initialState)
                .payload(payload != null ? payload : new HashMap<>())
                .build();
    }

    /**
     * Transitions the saga to a new state.
     */
    public void transitionTo(String newState) {
        this.previousState = this.currentState;
        this.currentState = newState;
        this.lastUpdatedAt = Instant.now();
    }

    /**
     * Records a completed step.
     */
    public void recordStepCompletion(String stepName, Object result) {
        if (this.completedSteps == null) {
            this.completedSteps = new java.util.ArrayList<>();
        }
        if (this.stepContext == null) {
            this.stepContext = new HashMap<>();
        }
        this.completedSteps.add(stepName);
        this.stepContext.put(stepName, result);
        this.lastUpdatedAt = Instant.now();
    }

    /**
     * Marks the saga as completed successfully.
     */
    public void complete() {
        this.completedAt = Instant.now();
        this.lastUpdatedAt = Instant.now();
    }

    /**
     * Records an error in the saga execution.
     */
    public void recordError(String step, String message) {
        this.errorStep = step;
        this.errorMessage = message;
        this.lastUpdatedAt = Instant.now();
    }

    /**
     * Increments retry count.
     */
    public void incrementRetry() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }

    /**
     * Checks if max retries have been exceeded.
     */
    public boolean isMaxRetriesExceeded() {
        return this.retryCount != null && this.maxRetries != null && this.retryCount >= this.maxRetries;
    }

    /**
     * Gets the steps in reverse order for compensation.
     */
    public java.util.List<String> getStepsForCompensation() {
        if (this.completedSteps == null || this.completedSteps.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        java.util.List<String> reversed = new java.util.ArrayList<>(this.completedSteps);
        java.util.Collections.reverse(reversed);
        return reversed;
    }
}
