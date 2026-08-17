package id.payu.wallet.application.service;

import id.payu.wallet.domain.model.JournalEntry;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.domain.model.ChartOfAccount;
import id.payu.wallet.domain.port.in.JournalUseCase;
import id.payu.wallet.domain.port.out.JournalPersistencePort;
import id.payu.wallet.interfaces.dto.TrialBalanceResponse;
import id.payu.wallet.interfaces.dto.TrialBalanceResponse.TrialBalanceEntry;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import id.payu.wallet.domain.model.EntryType;
import id.payu.wallet.domain.model.JournalStatus;

/**
 * Application service for double-entry journal operations (IMP-001).
 * Enforces the accounting invariant: sum(debit) == sum(credit) per journal.
 */
@Service
public class JournalService implements JournalUseCase {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JournalService.class);

    private final JournalPersistencePort journalPersistencePort;

    public JournalService(JournalPersistencePort journalPersistencePort) {
        this.journalPersistencePort = journalPersistencePort;
    }

    @Override
    @Transactional
    public JournalEntry createAndPostJournal(String description, String referenceType,
                                              String referenceId, List<LedgerEntry> entries,
                                              String createdBy) {
        if (entries == null || entries.size() < 2) {
            throw new IllegalArgumentException("A journal must have at least 2 entries (debit + credit)");
        }

        log.info("Creating journal entry: desc={}, refType={}, refId={}, entries={}",
                description, referenceType, referenceId, entries.size());

        String journalNumber = journalPersistencePort.generateJournalNumber();

        JournalEntry journal = JournalEntry.builder()
                .journalNumber(journalNumber)
                .description(description)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .status(JournalStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .createdBy(createdBy)
                .entries(new ArrayList<>())
                .build();

        // Assign IDs and journal reference to each entry
        UUID transactionId = UUID.randomUUID();
        for (LedgerEntry entry : entries) {
            if (entry.getTransactionId() == null) {
                entry.setTransactionId(transactionId);
            }
            if (entry.getCreatedAt() == null) {
                entry.setCreatedAt(LocalDateTime.now());
            }
            journal.addEntry(entry);
        }

        // Validate and post — throws if not balanced
        journal.post();

        // Persist
        JournalEntry saved = journalPersistencePort.saveJournal(journal);

        log.info("Journal {} posted: debit={}, credit={}, entries={}",
                journalNumber, journal.getTotalDebit(), journal.getTotalCredit(), entries.size());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public JournalEntry getJournal(UUID journalId) {
        log.debug("Getting journal by ID: {}", journalId);
        return journalPersistencePort.findJournalById(journalId)
                .orElseThrow(() -> new IllegalArgumentException("Journal not found: " + journalId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalEntry> getJournalsByReference(String referenceType, String referenceId) {
        log.debug("Getting journals by reference: type={}, id={}", referenceType, referenceId);
        return journalPersistencePort.findJournalsByReference(referenceType, referenceId);
    }

    @Override
    @Transactional(readOnly = true)
    public TrialBalanceResponse getTrialBalance() {
        log.info("Generating trial balance (all time)");
        return buildTrialBalance(null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public TrialBalanceResponse getTrialBalance(LocalDate from, LocalDate to) {
        log.info("Generating trial balance: from={}, to={}", from, to);
        return buildTrialBalance(from, to);
    }

    private TrialBalanceResponse buildTrialBalance(LocalDate from, LocalDate to) {
        // Retrieve all ledger entries (optionally filtered by date)
        List<LedgerEntry> allEntries = journalPersistencePort.findAllLedgerEntries();

        if (from != null && to != null) {
            LocalDateTime fromDateTime = from.atStartOfDay();
            LocalDateTime toDateTime = to.plusDays(1).atStartOfDay();
            allEntries = allEntries.stream()
                    .filter(e -> e.getCreatedAt() != null
                            && !e.getCreatedAt().isBefore(fromDateTime)
                            && e.getCreatedAt().isBefore(toDateTime))
                    .collect(Collectors.toList());
        }

        // Group by CoA code (or accountId if no CoA code set)
        Map<String, List<LedgerEntry>> grouped = allEntries.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCoaCode() != null ? e.getCoaCode() : "UNCLASSIFIED"
                ));

        List<TrialBalanceEntry> tbEntries = new ArrayList<>();
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (Map.Entry<String, List<LedgerEntry>> group : grouped.entrySet()) {
            String coaCode = group.getKey();
            List<LedgerEntry> entries = group.getValue();

            BigDecimal debitTotal = entries.stream()
                    .filter(e -> e.getEntryType() == EntryType.DEBIT)
                    .map(LedgerEntry::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal creditTotal = entries.stream()
                    .filter(e -> e.getEntryType() == EntryType.CREDIT)
                    .map(LedgerEntry::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Resolve account name from CoA
            String accountName = coaCode;
            String accountType = "UNKNOWN";
            Optional<ChartOfAccount> coa = journalPersistencePort.findChartOfAccountByCode(coaCode);
            if (coa.isPresent()) {
                accountName = coa.get().getName();
                accountType = coa.get().getAccountType().name();
            }

            tbEntries.add(new TrialBalanceEntry(
                    coaCode, accountName, accountType,
                    debitTotal, creditTotal, debitTotal.subtract(creditTotal)
            ));

            totalDebits = totalDebits.add(debitTotal);
            totalCredits = totalCredits.add(creditTotal);
        }

        // Sort by CoA code
        tbEntries.sort(Comparator.comparing(TrialBalanceEntry::getCoaCode));

        return TrialBalanceResponse.builder()
                .reportDate(LocalDate.now())
                .periodFrom(from)
                .periodTo(to)
                .entries(tbEntries)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .balanced(totalDebits.compareTo(totalCredits) == 0)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
