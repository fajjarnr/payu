package id.payu.wallet.domain.port.out;

import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.model.WalletTransaction;
import id.payu.wallet.domain.model.LedgerEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for wallet persistence operations.
 */
public interface WalletPersistencePort {
    
    Wallet save(Wallet wallet);
    
    Optional<Wallet> findById(UUID walletId);
    
    Optional<Wallet> findByAccountId(String accountId);

    /**
     * Find wallet by account ID with pessimistic write lock.
     * Must be called within an active transaction.
     * Used for balance-modifying operations to prevent concurrent update races.
     */
    Optional<Wallet> findByAccountIdForUpdate(String accountId);
    
    WalletTransaction saveTransaction(WalletTransaction transaction);

    Optional<WalletTransaction> findTransactionByReference(String referenceId);
    
    List<WalletTransaction> findTransactionsByWalletId(UUID walletId, int page, int size);
    
    default List<WalletTransaction> findTransactionsByWalletIdKeyset(UUID walletId, java.time.LocalDateTime lastCreatedAt, UUID lastId, int limit) {
        return findTransactionsByWalletId(walletId, 0, limit);
    }

    LedgerEntry saveLedgerEntry(LedgerEntry entry);

    Optional<LedgerEntry> findReservationByReference(String referenceId);

    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(String accountId);

    List<LedgerEntry> findByTransactionId(UUID transactionId);
}
