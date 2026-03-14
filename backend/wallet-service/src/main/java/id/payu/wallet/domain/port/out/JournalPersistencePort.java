package id.payu.wallet.domain.port.out;

import id.payu.wallet.domain.model.ChartOfAccount;
import id.payu.wallet.domain.model.JournalEntry;
import id.payu.wallet.domain.model.LedgerEntry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalPersistencePort {
    JournalEntry saveJournal(JournalEntry journal);
    Optional<JournalEntry> findJournalById(UUID id);
    List<JournalEntry> findJournalsByReference(String referenceType, String referenceId);
    List<JournalEntry> findJournalsByPostedAtBetween(LocalDateTime from, LocalDateTime to);

    List<LedgerEntry> findAllLedgerEntries();
    List<LedgerEntry> findLedgerEntriesByCoaCode(String coaCode);
    List<LedgerEntry> findLedgerEntriesByCoaCodeAndDateRange(String coaCode, LocalDateTime from, LocalDateTime to);

    ChartOfAccount saveChartOfAccount(ChartOfAccount coa);
    Optional<ChartOfAccount> findChartOfAccountByCode(String code);
    List<ChartOfAccount> findAllActiveChartOfAccounts();
    List<ChartOfAccount> findChartOfAccountsByType(String accountType);
    List<ChartOfAccount> findChartOfAccountsByParentId(UUID parentId);
    boolean chartOfAccountExistsByCode(String code);
    String generateJournalNumber();
}
