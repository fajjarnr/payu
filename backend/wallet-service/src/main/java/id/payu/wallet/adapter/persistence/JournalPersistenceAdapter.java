package id.payu.wallet.adapter.persistence;

import id.payu.wallet.adapter.persistence.entity.ChartOfAccountEntity;
import id.payu.wallet.adapter.persistence.entity.JournalEntryEntity;
import id.payu.wallet.adapter.persistence.entity.LedgerEntryEntity;
import id.payu.wallet.adapter.persistence.repository.ChartOfAccountJpaRepository;
import id.payu.wallet.adapter.persistence.repository.JournalEntryJpaRepository;
import id.payu.wallet.adapter.persistence.repository.LedgerEntryJpaRepository;
import id.payu.wallet.domain.model.ChartOfAccount;
import id.payu.wallet.domain.model.JournalEntry;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.port.out.JournalPersistencePort;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import id.payu.wallet.adapter.persistence.entity.JournalStatus;
import id.payu.wallet.domain.model.AccountCategory;
import id.payu.wallet.domain.model.AccountType;
import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.NormalBalance;

/**
 * Persistence adapter for Journal and Chart of Account operations.
 */
@Component
public class JournalPersistenceAdapter implements JournalPersistencePort {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JournalPersistenceAdapter.class);

    private final JournalEntryJpaRepository journalRepository;
    private final LedgerEntryJpaRepository ledgerEntryRepository;
    private final ChartOfAccountJpaRepository coaRepository;
    private final AtomicLong journalSequence = new AtomicLong(System.currentTimeMillis());

    public JournalPersistenceAdapter(JournalEntryJpaRepository journalRepository,
                                      LedgerEntryJpaRepository ledgerEntryRepository,
                                      ChartOfAccountJpaRepository coaRepository) {
        this.journalRepository = journalRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.coaRepository = coaRepository;
    }

    @Override
    public JournalEntry saveJournal(JournalEntry journal) {
        JournalEntryEntity entity = toJournalEntity(journal);
        JournalEntryEntity saved = journalRepository.save(entity);

        // Save associated ledger entries
        for (LedgerEntry entry : journal.getEntries()) {
            LedgerEntryEntity ledgerEntity = toLedgerEntity(entry);
            ledgerEntity.setJournalEntry(saved);
            ledgerEntryRepository.save(ledgerEntity);
        }

        return toJournalDomain(saved, journal.getEntries());
    }

    @Override
    public Optional<JournalEntry> findJournalById(UUID id) {
        return journalRepository.findById(id)
                .map(entity -> {
                    List<LedgerEntry> entries = entity.getEntries().stream()
                            .map(this::toLedgerDomain)
                            .collect(Collectors.toList());
                    return toJournalDomain(entity, entries);
                });
    }

    @Override
    public List<JournalEntry> findJournalsByReference(String referenceType, String referenceId) {
        return journalRepository.findByReference(referenceType, referenceId)
                .stream()
                .map(entity -> {
                    List<LedgerEntry> entries = entity.getEntries().stream()
                            .map(this::toLedgerDomain)
                            .collect(Collectors.toList());
                    return toJournalDomain(entity, entries);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<JournalEntry> findJournalsByPostedAtBetween(LocalDateTime from, LocalDateTime to) {
        return journalRepository.findByPostedAtBetween(from, to)
                .stream()
                .map(entity -> {
                    List<LedgerEntry> entries = entity.getEntries().stream()
                            .map(this::toLedgerDomain)
                            .collect(Collectors.toList());
                    return toJournalDomain(entity, entries);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<LedgerEntry> findAllLedgerEntries() {
        return ledgerEntryRepository.findAll().stream()
                .map(this::toLedgerDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<LedgerEntry> findLedgerEntriesByCoaCode(String coaCode) {
        return ledgerEntryRepository.findByCoaCode(coaCode).stream()
                .map(this::toLedgerDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<LedgerEntry> findLedgerEntriesByCoaCodeAndDateRange(String coaCode,
                                                                     LocalDateTime from,
                                                                     LocalDateTime to) {
        return ledgerEntryRepository.findByCoaCodeAndCreatedAtBetween(coaCode, from, to).stream()
                .map(this::toLedgerDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<LedgerEntry> findLedgerEntriesByReferenceIds(List<String> referenceIds) {
        if (referenceIds == null || referenceIds.isEmpty()) {
            return List.of();
        }
        return ledgerEntryRepository.findByReferenceIdIn(referenceIds).stream()
                .map(this::toLedgerDomain)
                .collect(Collectors.toList());
    }

    @Override
    public ChartOfAccount saveChartOfAccount(ChartOfAccount coa) {
        ChartOfAccountEntity saved = coaRepository.save(toCoaEntity(coa));
        return toCoaDomain(saved);
    }

    @Override
    public Optional<ChartOfAccount> findChartOfAccountByCode(String code) {
        return coaRepository.findByCode(code).map(this::toCoaDomain);
    }

    @Override
    public List<ChartOfAccount> findAllActiveChartOfAccounts() {
        return coaRepository.findAllActive().stream()
                .map(this::toCoaDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChartOfAccount> findChartOfAccountsByType(String accountType) {
        return coaRepository.findByAccountType(accountType).stream()
                .map(this::toCoaDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChartOfAccount> findChartOfAccountsByParentId(UUID parentId) {
        return coaRepository.findByParentId(parentId).stream()
                .map(this::toCoaDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean chartOfAccountExistsByCode(String code) {
        return coaRepository.existsByCode(code);
    }

    @Override
    public String generateJournalNumber() {
        LocalDate today = LocalDate.now();
        long seq = journalSequence.incrementAndGet() % 100000;
        return String.format("JRN-%s-%05d",
                today.toString().replace("-", ""),
                seq);
    }

    // ---- Mappers ----

    private JournalEntryEntity toJournalEntity(JournalEntry domain) {
        return JournalEntryEntity.builder()
                .id(domain.getId())
                .journalNumber(domain.getJournalNumber())
                .description(domain.getDescription())
                .referenceType(domain.getReferenceType())
                .referenceId(domain.getReferenceId())
                .status(JournalStatus.valueOf(domain.getStatus().name()))
                .postedAt(domain.getPostedAt())
                .createdAt(domain.getCreatedAt())
                .createdBy(domain.getCreatedBy())
                .build();
    }

    private JournalEntry toJournalDomain(JournalEntryEntity entity, List<LedgerEntry> entries) {
        return JournalEntry.builder()
                .id(entity.getId())
                .journalNumber(entity.getJournalNumber())
                .description(entity.getDescription())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .status(id.payu.wallet.domain.model.JournalStatus.valueOf(entity.getStatus().name()))
                .postedAt(entity.getPostedAt())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .entries(entries)
                .build();
    }

    private LedgerEntryEntity toLedgerEntity(LedgerEntry entry) {
        return LedgerEntryEntity.builder()
                .id(entry.getId())
                .transactionId(entry.getTransactionId())
                .accountId(entry.getAccountId())
                .coaCode(entry.getCoaCode())
                .entryType(entry.getEntryType())
                .amount(entry.getAmount())
                .currency(entry.getCurrency())
                .balanceAfter(entry.getBalanceAfter())
                .referenceType(entry.getReferenceType())
                .referenceId(entry.getReferenceId())
                .createdAt(entry.getCreatedAt())
                .build();
    }

    private LedgerEntry toLedgerDomain(LedgerEntryEntity entity) {
        return LedgerEntry.builder()
                .id(entity.getId())
                .transactionId(entity.getTransactionId())
                .journalEntryId(entity.getJournalEntry() != null ? entity.getJournalEntry().getId() : null)
                .accountId(entity.getAccountId())
                .coaCode(entity.getCoaCode())
                .entryType(entity.getEntryType())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .balanceAfter(entity.getBalanceAfter())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private ChartOfAccountEntity toCoaEntity(ChartOfAccount domain) {
        return ChartOfAccountEntity.builder()
                .id(domain.getId())
                .code(domain.getCode())
                .name(domain.getName())
                .description(domain.getDescription())
                .accountType(domain.getAccountType().name())
                .category(domain.getCategory() != null ? domain.getCategory().name() : null)
                .parentId(domain.getParentId())
                .level(domain.getLevel())
                .active(domain.isActive())
                .normalBalance(domain.getNormalBalance().name())
                .currency(domain.getCurrency())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private ChartOfAccount toCoaDomain(ChartOfAccountEntity entity) {
        return ChartOfAccount.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .accountType(AccountType.valueOf(entity.getAccountType()))
                .category(entity.getCategory() != null
                        ? AccountCategory.valueOf(entity.getCategory()) : null)
                .parentId(entity.getParentId())
                .level(entity.getLevel())
                .active(entity.isActive())
                .normalBalance(NormalBalance.valueOf(entity.getNormalBalance()))
                .currency(entity.getCurrency())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
