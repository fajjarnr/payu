package id.payu.wallet.domain.port.in;

import java.math.BigDecimal;

/**
 * ADR-0029 interbank clearing suspense ledgering input port.
 * Each lifecycle stage posts exactly one immutable, balanced double-entry
 * journal keyed by (stage reference_type, referenceId); replays with the same
 * referenceId are idempotent no-ops returning the original journal number.
 */
public interface WalletClearingUseCase {

    /**
     * Outbound clearing hold (pacs.008 initiation):
     * DEBIT user CASA (amount + fee), CREDIT suspense clearing account,
     * CREDIT transfer fee revenue.
     *
     * @return journal number of the posted (or previously posted) hold journal
     */
    String reserveAndHoldClearing(String accountId, String clearingAccountSlug,
                                  BigDecimal amount, BigDecimal fee,
                                  String referenceId, String description);

    /**
     * Settlement confirmed (pacs.002 ACTC):
     * DEBIT suspense clearing account, CREDIT Nostro cash-at-central-bank account.
     */
    void settleClearing(String clearingAccountSlug, String settlementAccountSlug,
                        BigDecimal amount, String referenceId);

    /**
     * Settlement rejected/timeout (pacs.002 RJCT / pacs.004):
     * DEBIT suspense clearing account + DEBIT fee revenue, CREDIT user CASA (amount + fee).
     */
    void reverseClearing(String clearingAccountSlug, String accountId,
                         BigDecimal amount, BigDecimal fee,
                         String referenceId, String reason);
}
