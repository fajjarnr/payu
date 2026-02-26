package id.payu.billing.exception;

/**
 * Exception thrown when a subscription or plan is not found.
 */
public class SubscriptionNotFoundException extends RuntimeException {

    public SubscriptionNotFoundException(String message) {
        super(message);
    }
}
