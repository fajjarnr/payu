package id.payu.wallet.application.service;

/**
 * Exception thrown when a settlement batch is not found.
 */
// TODO BUG-ARCH-002: Migrate to extend BusinessException with proper error codes
public class SettlementNotFoundException extends RuntimeException {

    public SettlementNotFoundException(String batchId) {
        super("Settlement batch not found: " + batchId);
    }
}
