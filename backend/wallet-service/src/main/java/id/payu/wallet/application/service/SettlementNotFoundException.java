package id.payu.wallet.application.service;

/**
 * Exception thrown when a settlement batch is not found.
 */
public class SettlementNotFoundException extends RuntimeException {

    public SettlementNotFoundException(String batchId) {
        super("Settlement batch not found: " + batchId);
    }
}
