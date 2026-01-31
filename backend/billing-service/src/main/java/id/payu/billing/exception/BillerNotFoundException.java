package id.payu.billing.exception;

/**
 * Exception thrown when a biller is not found.
 */
public class BillerNotFoundException extends RuntimeException {

    public BillerNotFoundException(String message) {
        super(message);
    }
}
