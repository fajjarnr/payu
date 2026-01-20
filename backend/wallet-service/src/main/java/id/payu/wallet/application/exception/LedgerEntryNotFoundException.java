package id.payu.wallet.application.exception;

public class LedgerEntryNotFoundException extends RuntimeException {
    public LedgerEntryNotFoundException(String transactionId) {
        super("Ledger entry not found for transaction: " + transactionId);
    }
}
