package id.payu.wallet.application.exception;

// TODO BUG-ARCH-002: Migrate to extend BusinessException with proper error codes
public class LedgerEntryNotFoundException extends RuntimeException {
    public LedgerEntryNotFoundException(String transactionId) {
        super("Ledger entry not found for transaction: " + transactionId);
    }
}
