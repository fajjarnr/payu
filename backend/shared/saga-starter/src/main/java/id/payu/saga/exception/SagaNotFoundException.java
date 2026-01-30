package id.payu.saga.exception;

/**
 * Exception thrown when a saga instance is not found.
 */
public class SagaNotFoundException extends SagaException {

    public SagaNotFoundException(String sagaId) {
        super("Saga instance not found: " + sagaId, sagaId, null);
    }

    public SagaNotFoundException(String sagaId, String sagaType) {
        super("Saga instance not found: " + sagaId, sagaId, sagaType);
    }
}
