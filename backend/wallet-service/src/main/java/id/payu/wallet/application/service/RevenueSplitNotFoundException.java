package id.payu.wallet.application.service;

/**
 * Exception thrown when a revenue split is not found.
 */
// TODO BUG-ARCH-002: Migrate to extend BusinessException with proper error codes
public class RevenueSplitNotFoundException extends RuntimeException {

    public RevenueSplitNotFoundException(String splitId) {
        super("Revenue split not found: " + splitId);
    }
}
