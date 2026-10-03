package id.payu.saga.model;

/**
 * Standard saga states for orchestration lifecycle.
 * Services can extend these with their own specific states.
 */
public enum SagaState {

    PENDING("Saga created but not yet started"),

    STARTED("Saga execution started"),
    IN_PROGRESS("Saga steps executing"),
    WAITING_FOR_RESPONSE("Waiting for external service response"),

    COMPLETED("Saga completed successfully"),

    FAILED("Saga failed, compensation may be needed"),
    COMPENSATING("Compensation in progress"),
    COMPENSATED("Compensation completed"),
    COMPENSATION_FAILED("Compensation failed - requires manual intervention"),

    TIMED_OUT("Saga timed out"),
    RETRYING("Retrying failed step"),

    PAUSED("Saga paused for manual review"),
    CANCELLED("Saga cancelled by user/system");

    private final String description;

    SagaState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this is a terminal state (no further transitions expected).
     */
    public boolean isTerminal() {
        return this == COMPLETED ||
               this == FAILED ||
               this == COMPENSATED ||
               this == COMPENSATION_FAILED ||
               this == CANCELLED;
    }

    /**
     * Check if compensation is in progress or completed.
     */
    public boolean isCompensating() {
        return this == COMPENSATING ||
               this == COMPENSATED ||
               this == COMPENSATION_FAILED;
    }

    public boolean isRetryable() {
        return this == FAILED ||
               this == TIMED_OUT ||
               this == RETRYING;
    }

    public String value() {
        return this.name();
    }
}
