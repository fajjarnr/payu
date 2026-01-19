package id.payu.wallet.domain.port.in;

import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.model.WalletTransaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Input port defining wallet use cases.
 * This is the primary interface for the application layer.
 */
public interface WalletUseCase {

    /**
     * Get wallet by account ID.
     */
    Optional<Wallet> getWalletByAccountId(String accountId);

    /**
     * Get current balance for an account.
     */
    BigDecimal getBalance(String accountId);

    /**
     * Get available balance (total - reserved) for an account.
     */
    BigDecimal getAvailableBalance(String accountId);

    /**
     * Reserve balance for a pending transaction.
     * 
     * @param accountId the account ID
     * @param amount the amount to reserve
     * @param referenceId external reference (e.g., transaction ID)
     * @return reservation ID for later commit/release
     */
    String reserveBalance(String accountId, BigDecimal amount, String referenceId);

    /**
     * Commit a previously reserved amount (deduct from balance).
     * 
     * @param reservationId the reservation ID from reserveBalance
     */
    void commitReservation(String reservationId);

    /**
     * Release a previously reserved amount back to available.
     * 
     * @param reservationId the reservation ID from reserveBalance
     */
    void releaseReservation(String reservationId);

    /**
     * Credit amount to wallet (incoming transfer).
     * 
     * @param accountId the account to credit
     * @param amount the amount to credit
     * @param referenceId external reference
     * @param description transaction description
     */
    void credit(String accountId, BigDecimal amount, String referenceId, String description);

    /**
     * Get transaction history for a wallet.
     */
    List<WalletTransaction> getTransactionHistory(String accountId, int page, int size);
}
