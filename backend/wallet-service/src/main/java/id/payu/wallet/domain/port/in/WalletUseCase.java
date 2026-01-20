package id.payu.wallet.domain.port.in;

import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.model.WalletTransaction;
import id.payu.wallet.domain.model.LedgerEntry;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port defining wallet use cases.
 * This is the primary interface for the application layer.
 */
public interface WalletUseCase {
    
    /**
     * Get wallet by account ID.
     * @param accountId   account ID
     * @return            wallet
     */
    Optional<Wallet> getWalletByAccountId(String accountId);
    
    /**
     * Get wallet by wallet ID.
     * @param walletId    wallet ID
     * @return            wallet
     */
    Wallet getWallet(UUID walletId);
    
    /**
     * Create a new wallet for an account.
     * @param accountId   account ID
     * @return            created wallet
     */
    Wallet createWallet(String accountId);
    
    /**
     * Get current balance for an account.
     * @param accountId   account ID
     * @return            balance
     */
    BigDecimal getBalance(String accountId);
    
    /**
     * Get available balance (total - reserved) for an account.
     * @param accountId   account ID
     * @return            available balance
     */
    BigDecimal getAvailableBalance(String accountId);
    
    /**
     * Reserve balance for a pending transaction.
     * @param accountId   account ID
     * @param amount      amount to reserve
     * @param referenceId external reference (e.g., transaction ID)
     * @return            reservation ID for later commit/release
     */
    String reserveBalance(String accountId, BigDecimal amount, String referenceId);
    
    /**
     * Commit a previously reserved amount (deduct from balance).
     * @param reservationId reservation ID from reserveBalance
     */
    void commitReservation(String reservationId);
    
    /**
     * Release a previously reserved amount back to available.
     * @param reservationId reservation ID from reserveBalance
     */
    void releaseReservation(String reservationId);
    
    /**
     * Credit amount to wallet (incoming transfer).
     * @param accountId   account to credit
     * @param amount      amount to credit
     * @param referenceId external reference
     * @param description transaction description
     */
    void credit(String accountId, BigDecimal amount, String referenceId, String description);
    
    /**
     * Get transaction history for a wallet.
     * @param accountId   account ID
     * @param page      page number
     * @param size       page size
     * @return            transaction history
     */
    List<WalletTransaction> getTransactionHistory(String accountId, int page, int size);
    
    List<LedgerEntry> getLedgerEntriesByAccountId(UUID accountId);
    
    List<LedgerEntry> getLedgerEntriesByTransactionId(UUID transactionId);
}
