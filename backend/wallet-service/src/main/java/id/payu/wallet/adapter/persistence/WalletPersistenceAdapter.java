package id.payu.wallet.adapter.persistence;

import id.payu.wallet.adapter.persistence.entity.WalletEntity;
import id.payu.wallet.adapter.persistence.entity.WalletTransactionEntity;
import id.payu.wallet.adapter.persistence.repository.WalletJpaRepository;
import id.payu.wallet.adapter.persistence.repository.WalletTransactionJpaRepository;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.model.WalletTransaction;
import id.payu.wallet.domain.port.out.WalletPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter implementing WalletPersistencePort.
 * Maps between domain models and JPA entities.
 */
@Component
@RequiredArgsConstructor
public class WalletPersistenceAdapter implements WalletPersistencePort {

    private final WalletJpaRepository walletRepository;
    private final WalletTransactionJpaRepository transactionRepository;

    @Override
    public Optional<Wallet> findByAccountId(String accountId) {
        return walletRepository.findByAccountId(accountId)
                .map(this::toDomain);
    }

    @Override
    public Optional<Wallet> findById(UUID walletId) {
        return walletRepository.findById(walletId)
                .map(this::toDomain);
    }

    @Override
    public Wallet save(Wallet wallet) {
        WalletEntity entity = toEntity(wallet);
        WalletEntity saved = walletRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public WalletTransaction saveTransaction(WalletTransaction transaction) {
        WalletTransactionEntity entity = toTransactionEntity(transaction);
        WalletTransactionEntity saved = transactionRepository.save(entity);
        return toTransactionDomain(saved);
    }

    @Override
    public List<WalletTransaction> findTransactionsByWalletId(UUID walletId, int page, int size) {
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId, PageRequest.of(page, size))
                .stream()
                .map(this::toTransactionDomain)
                .toList();
    }

    // Mapping methods
    private Wallet toDomain(WalletEntity entity) {
        return Wallet.builder()
                .id(entity.getId())
                .accountId(entity.getAccountId())
                .balance(entity.getBalance())
                .reservedBalance(entity.getReservedBalance())
                .currency(entity.getCurrency())
                .status(Wallet.WalletStatus.valueOf(entity.getStatus().name()))
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private WalletEntity toEntity(Wallet wallet) {
        return WalletEntity.builder()
                .id(wallet.getId())
                .accountId(wallet.getAccountId())
                .balance(wallet.getBalance())
                .reservedBalance(wallet.getReservedBalance())
                .currency(wallet.getCurrency())
                .status(WalletEntity.WalletStatus.valueOf(wallet.getStatus().name()))
                .version(wallet.getVersion())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    private WalletTransaction toTransactionDomain(WalletTransactionEntity entity) {
        return WalletTransaction.builder()
                .id(entity.getId())
                .walletId(entity.getWalletId())
                .referenceId(entity.getReferenceId())
                .type(WalletTransaction.TransactionType.valueOf(entity.getType().name()))
                .amount(entity.getAmount())
                .balanceAfter(entity.getBalanceAfter())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private WalletTransactionEntity toTransactionEntity(WalletTransaction transaction) {
        return WalletTransactionEntity.builder()
                .id(transaction.getId())
                .walletId(transaction.getWalletId())
                .referenceId(transaction.getReferenceId())
                .type(WalletTransactionEntity.TransactionType.valueOf(transaction.getType().name()))
                .amount(transaction.getAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
