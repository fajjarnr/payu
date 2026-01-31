package id.payu.billing.exception;

/**
 * Exception thrown when a top-up transaction is not found.
 */
public class TopUpNotFoundException extends RuntimeException {

    public TopUpNotFoundException(String message) {
        super(message);
    }
}
