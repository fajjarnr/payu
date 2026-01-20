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
    
    WalletTransaction saveTransaction(WalletTransaction transaction);
    
    List<WalletTransaction> findTransactionsByWalletId(UUID walletId, int page, int size);

    LedgerEntry saveLedgerEntry(LedgerEntry entry);

    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    List<LedgerEntry> findByTransactionId(UUID transactionId);
}
