package id.payu.saga.exception;

import java.util.List;

/**
 * Exception thrown when saga compensation fails.
 */
public class CompensationException extends SagaException {

    private final List<String> compensatedSteps;
    private final List<String> failedSteps;

    public CompensationException(String sagaId, String sagaType, String message) {
        super(message, sagaId, sagaType, "COMPENSATION");
        this.compensatedSteps = null;
        this.failedSteps = null;
    }

    public CompensationException(String sagaId, String sagaType, String message,
                                  List<String> compensatedSteps, List<String> failedSteps) {
        super(message, sagaId, sagaType, "COMPENSATION");
        this.compensatedSteps = compensatedSteps;
        this.failedSteps = failedSteps;
    }

    public CompensationException(String sagaId, String sagaType, String message, Throwable cause) {
        super(message, cause, sagaId, sagaType, "COMPENSATION");
        this.compensatedSteps = null;
        this.failedSteps = null;
    }

    public List<String> getCompensatedSteps() {
        return compensatedSteps;
    }

    public List<String> getFailedSteps() {
        return failedSteps;
    }
}
