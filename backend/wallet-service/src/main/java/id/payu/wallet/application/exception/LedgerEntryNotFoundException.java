package id.payu.wallet.application.exception;

import id.payu.api.common.exception.BusinessException;

/**
 * BUG-ARCH-002 FIX: Migrated to extend BusinessException with proper error code WAL_004.
 * Thrown when a ledger entry is not found for a given transaction.
 */
public class LedgerEntryNotFoundException extends BusinessException {
    public LedgerEntryNotFoundException(String transactionId) {
        super("WAL_004", "Ledger entry not found for transaction: " + transactionId);
    }
}
