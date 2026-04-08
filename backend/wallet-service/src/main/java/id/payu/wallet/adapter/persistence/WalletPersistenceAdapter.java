package id.payu.wallet.adapter.persistence;

import id.payu.wallet.adapter.persistence.entity.WalletEntity;
import id.payu.wallet.adapter.persistence.entity.WalletTransactionEntity;
import id.payu.wallet.adapter.persistence.entity.LedgerEntryEntity;
import id.payu.wallet.adapter.persistence.mapper.LedgerEntryMapper;
import id.payu.wallet.adapter.persistence.mapper.WalletMapper;
import id.payu.wallet.adapter.persistence.repository.WalletJpaRepository;
import id.payu.wallet.adapter.persistence.repository.WalletTransactionJpaRepository;
import id.payu.wallet.adapter.persistence.repository.LedgerEntryJpaRepository;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.model.WalletTransaction;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.port.out.WalletPersistencePort;
import id.payu.wallet.multitenancy.TenantContext;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Persistence adapter for Wallet domain using MapStruct mappers.
 *
 * <p>IMP-069: Uses MapStruct mappers ({@link WalletMapper}, {@link LedgerEntryMapper})
 * instead of manual mapping, reducing ~100 lines of boilerplate code.</p>
 *
 * @see WalletMapper
 * @see LedgerEntryMapper
 * @since IMP-069
 */
@Component
public class WalletPersistenceAdapter implements WalletPersistencePort {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WalletPersistenceAdapter.class);

    private final WalletJpaRepository walletRepository;
    private final WalletTransactionJpaRepository transactionRepository;
    private final LedgerEntryJpaRepository ledgerEntryRepository;
    private final WalletMapper walletMapper;
    private final LedgerEntryMapper ledgerEntryMapper;

    public WalletPersistenceAdapter(WalletJpaRepository walletRepository,
                                    WalletTransactionJpaRepository transactionRepository,
                                    LedgerEntryJpaRepository ledgerEntryRepository,
                                    WalletMapper walletMapper,
                                    LedgerEntryMapper ledgerEntryMapper) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.walletMapper = walletMapper;
        this.ledgerEntryMapper = ledgerEntryMapper;
    }

    @Override
    public Wallet save(Wallet wallet) {
        WalletEntity entity = walletMapper.toEntity(wallet);

        if (wallet.getId() != null) {
            walletRepository.findById(wallet.getId())
                    .map(WalletEntity::getTenantId)
                    .filter(tenantId -> tenantId != null && !tenantId.isBlank())
                    .ifPresent(entity::setTenantId);
        }

        if (entity.getTenantId() == null || entity.getTenantId().isBlank()) {
            entity.setTenantId(TenantContext.getTenantId());
        }

        WalletEntity savedEntity = walletRepository.saveAndFlush(entity);
        return walletMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Wallet> findById(UUID walletId) {
        return walletRepository.findById(walletId).map(walletMapper::toDomain);
    }

    @Override
    public Optional<Wallet> findByAccountId(String accountId) {
        return walletRepository.findByAccountId(accountId).map(walletMapper::toDomain);
    }

    @Override
    public Optional<Wallet> findByAccountIdForUpdate(String accountId) {
        return walletRepository.findByAccountIdForUpdate(accountId).map(walletMapper::toDomain);
    }

    @Override
    public WalletTransaction saveTransaction(WalletTransaction transaction) {
        WalletTransactionEntity savedEntity = transactionRepository.save(toTransactionEntity(transaction));
        return toTransactionDomain(savedEntity);
    }

    @Override
    public java.util.List<WalletTransaction> findTransactionsByWalletId(UUID walletId, int page, int size) {
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId, org.springframework.data.domain.PageRequest.of(page, size))
                .stream()
                .map(this::toTransactionDomain)
                .collect(Collectors.toList());
    }

    @Override
    public LedgerEntry saveLedgerEntry(LedgerEntry entry) {
        LedgerEntryEntity savedEntity = ledgerEntryRepository.save(ledgerEntryMapper.toEntity(entry));
        return ledgerEntryMapper.toDomain(savedEntity);
    }

    @Override
    public java.util.List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(String accountId) {
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(ledgerEntryMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public java.util.List<LedgerEntry> findByTransactionId(UUID transactionId) {
        return ledgerEntryRepository.findByTransactionId(transactionId)
                .stream()
                .map(ledgerEntryMapper::toDomain)
                .collect(Collectors.toList());
    }

    // Manual mapping for WalletTransaction (not yet migrated to MapStruct)
    private WalletTransactionEntity toTransactionEntity(WalletTransaction transaction) {
        return WalletTransactionEntity.builder()
                .id(transaction.getId())
                .walletId(transaction.getWalletId())
                .referenceId(transaction.getReferenceId())
                .type(toEntityType(transaction.getType()))
                .amount(transaction.getAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private WalletTransactionEntity.TransactionType toEntityType(WalletTransaction.TransactionType type) {
        return WalletTransactionEntity.TransactionType.valueOf(type.name());
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
}
