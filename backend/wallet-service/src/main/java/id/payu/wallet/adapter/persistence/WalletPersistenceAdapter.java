package id.payu.wallet.adapter.persistence;

import id.payu.wallet.adapter.persistence.entity.WalletEntity;
import id.payu.wallet.adapter.persistence.entity.WalletTransactionEntity;
import id.payu.wallet.adapter.persistence.entity.LedgerEntryEntity;
import id.payu.wallet.adapter.persistence.repository.WalletJpaRepository;
import id.payu.wallet.adapter.persistence.repository.WalletTransactionJpaRepository;
import id.payu.wallet.adapter.persistence.repository.LedgerEntryJpaRepository;
import id.payu.wallet.domain.model.Wallet;
import id.payu.wallet.domain.model.WalletTransaction;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.port.out.WalletPersistencePort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class WalletPersistenceAdapter implements WalletPersistencePort {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WalletPersistenceAdapter.class);

    private final WalletJpaRepository walletRepository;
    private final WalletTransactionJpaRepository transactionRepository;
    private final LedgerEntryJpaRepository ledgerEntryRepository;

    public WalletPersistenceAdapter(WalletJpaRepository walletRepository,
                                    WalletTransactionJpaRepository transactionRepository,
                                    LedgerEntryJpaRepository ledgerEntryRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Override
    public Wallet save(Wallet wallet) {
        WalletEntity savedEntity = walletRepository.save(toEntity(wallet));
        return toDomain(savedEntity);
    }

    @Override
    public Optional<Wallet> findById(UUID walletId) {
        return walletRepository.findById(walletId).map(this::toDomain);
    }

    @Override
    public Optional<Wallet> findByAccountId(String accountId) {
        return walletRepository.findByAccountId(accountId).map(this::toDomain);
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
        LedgerEntryEntity savedEntity = ledgerEntryRepository.save(toLedgerEntity(entry));
        return toDomain(savedEntity);
    }

    @Override
    public java.util.List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(UUID accountId) {
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public java.util.List<LedgerEntry> findByTransactionId(UUID transactionId) {
        return ledgerEntryRepository.findByTransactionId(transactionId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private WalletEntity toEntity(Wallet wallet) {
        return WalletEntity.builder()
                .id(wallet.getId())
                .accountId(wallet.getAccountId())
                .balance(wallet.getBalance())
                .reservedBalance(wallet.getReservedBalance())
                .currency(wallet.getCurrency())
                .status(toEntityStatus(wallet.getStatus()))
                .version(wallet.getVersion())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

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

    private LedgerEntryEntity toLedgerEntity(LedgerEntry entry) {
        return LedgerEntryEntity.builder()
                .id(entry.getId())
                .transactionId(entry.getTransactionId())
                .accountId(entry.getAccountId())
                .entryType(entry.getEntryType().name())
                .amount(entry.getAmount())
                .currency(entry.getCurrency())
                .balanceAfter(entry.getBalanceAfter())
                .referenceType(entry.getReferenceType())
                .referenceId(entry.getReferenceId())
                .createdAt(entry.getCreatedAt())
                .build();
    }

    private WalletEntity.WalletStatus toEntityStatus(Wallet.WalletStatus status) {
        return WalletEntity.WalletStatus.valueOf(status.name());
    }

    private WalletTransactionEntity.TransactionType toEntityType(WalletTransaction.TransactionType type) {
        return WalletTransactionEntity.TransactionType.valueOf(type.name());
    }

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

    private id.payu.wallet.domain.model.LedgerEntry toDomain(LedgerEntryEntity entity) {
        return id.payu.wallet.domain.model.LedgerEntry.builder()
                .id(entity.getId())
                .transactionId(entity.getTransactionId())
                .accountId(entity.getAccountId())
                .entryType(LedgerEntry.EntryType.valueOf(entity.getEntryType()))
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .balanceAfter(entity.getBalanceAfter())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

