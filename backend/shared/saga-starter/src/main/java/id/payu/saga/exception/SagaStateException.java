package id.payu.saga.exception;

import id.payu.saga.model.SagaState;

/**
 * Exception thrown when an invalid saga state transition is attempted.
 */
public class SagaStateException extends SagaException {

    private final String currentState;
    private final String attemptedState;

    public SagaStateException(String sagaId, String currentState, String attemptedState) {
        super(String.format("Invalid state transition from %s to %s", currentState, attemptedState),
              sagaId, null);
        this.currentState = currentState;
        this.attemptedState = attemptedState;
    }

    public SagaStateException(String sagaId, String sagaType, String currentState, String attemptedState) {
        super(String.format("Invalid state transition from %s to %s", currentState, attemptedState),
              sagaId, sagaType);
        this.currentState = currentState;
        this.attemptedState = attemptedState;
    }

    public String getCurrentState() {
        return currentState;
    }

    public String getAttemptedState() {
        return attemptedState;
    }
}
