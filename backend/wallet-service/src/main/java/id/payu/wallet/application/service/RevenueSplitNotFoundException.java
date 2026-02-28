package id.payu.wallet.application.service;

/**
 * Exception thrown when a revenue split is not found.
 */
public class RevenueSplitNotFoundException extends RuntimeException {

    public RevenueSplitNotFoundException(String splitId) {
        super("Revenue split not found: " + splitId);
    }
}
