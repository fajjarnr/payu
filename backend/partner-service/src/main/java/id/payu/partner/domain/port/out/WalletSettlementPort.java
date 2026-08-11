package id.payu.partner.domain.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Settles a payment between two wallet accounts.
 */
public interface WalletSettlementPort {

    void settle(String sourceAccountId, String beneficiaryAccountId,
                BigDecimal amount, String currency, String referenceId);

    void reverse(String senderAccountId, String recipientAccountId,
                 BigDecimal amount, String currency, UUID refundId, String description);

    /**
     * PARTNER-PROD-005: fetch wallet ledger movements for the given external
     * references (SNAP payment references / refund UUIDs) for reconciliation.
     */
    List<LedgerMovement> ledgerMovementsByReferences(List<String> referenceIds);

    /**
     * One wallet ledger movement relevant to reconciliation.
     */
    record LedgerMovement(String accountId, String referenceId, String referenceType,
                          String entryType, BigDecimal amount, BigDecimal balanceAfter) {
    }
}
