package id.payu.saga.exception;

/**
 * Base exception for saga-related errors.
 */
public class SagaException extends RuntimeException {

    private final String sagaId;
    private final String sagaType;
    private final String stepName;

    public SagaException(String message) {
        super(message);
        this.sagaId = null;
        this.sagaType = null;
        this.stepName = null;
    }

    public SagaException(String message, Throwable cause) {
        super(message, cause);
        this.sagaId = null;
        this.sagaType = null;
        this.stepName = null;
    }

    public SagaException(String message, String sagaId, String sagaType) {
        super(message);
        this.sagaId = sagaId;
        this.sagaType = sagaType;
        this.stepName = null;
    }

    public SagaException(String message, String sagaId, String sagaType, String stepName) {
        super(message);
        this.sagaId = sagaId;
        this.sagaType = sagaType;
        this.stepName = stepName;
    }

    public SagaException(String message, Throwable cause, String sagaId, String sagaType, String stepName) {
        super(message, cause);
        this.sagaId = sagaId;
        this.sagaType = sagaType;
        this.stepName = stepName;
    }

    public String getSagaId() {
        return sagaId;
    }

    public String getSagaType() {
        return sagaType;
    }

    public String getStepName() {
        return stepName;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        if (sagaId != null) {
            sb.append(" [sagaId=").append(sagaId).append("]");
        }
        if (sagaType != null) {
            sb.append(" [sagaType=").append(sagaType).append("]");
        }
        if (stepName != null) {
            sb.append(" [step=").append(stepName).append("]");
        }
        return sb.toString();
    }
}
