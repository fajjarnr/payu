package id.payu.wallet.interfaces.dto;

import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.LedgerEntry;
import java.math.BigDecimal;

/**
 * PARTNER-PROD-005: a single wallet ledger movement used by reconciliation.
 */
public record LedgerMovementResponse(
        String accountId,
        String referenceId,
        String referenceType,
        String entryType,
        BigDecimal amount,
        BigDecimal balanceAfter
) {
    public static LedgerMovementResponse from(LedgerEntry entry) {
        return new LedgerMovementResponse(
                entry.getAccountId(),
                entry.getReferenceId(),
                entry.getReferenceType(),
                entry.getEntryType() == null ? null : entry.getEntryType().name(),
                entry.getAmount(),
                entry.getBalanceAfter());
    }
}
