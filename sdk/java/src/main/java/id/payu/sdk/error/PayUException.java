package id.payu.sdk.error;

/**
 * Base unchecked exception thrown by the PayU Java SDK.
 */
public class PayUException extends RuntimeException {

    public PayUException(String message) {
        super(message);
    }

    public PayUException(String message, Throwable cause) {
        super(message, cause);
    }
}
