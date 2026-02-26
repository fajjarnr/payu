package id.payu.wallet.domain.port.in;

import id.payu.wallet.domain.model.JournalEntry;
import id.payu.wallet.domain.model.LedgerEntry;
import id.payu.wallet.dto.TrialBalanceResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Input port for journal/double-entry ledger use cases.
 */
public interface JournalUseCase {

    /**
     * Create and post a balanced journal entry with paired debit+credit entries.
     *
     * @param description   journal description
     * @param referenceType external reference type (e.g., TRANSFER, PAYMENT)
     * @param referenceId   external reference ID
     * @param entries       list of ledger entries (must balance: sum(debit) == sum(credit))
     * @param createdBy     the user or system creating this journal
     * @return the posted journal entry
     */
    JournalEntry createAndPostJournal(String description, String referenceType,
                                       String referenceId, List<LedgerEntry> entries,
                                       String createdBy);

    /**
     * Get a journal entry by ID including its ledger entries.
     */
    JournalEntry getJournal(UUID journalId);

    /**
     * Get journals by reference.
     */
    List<JournalEntry> getJournalsByReference(String referenceType, String referenceId);

    /**
     * Generate trial balance report — verifies sum(debit) == sum(credit) across all accounts.
     */
    TrialBalanceResponse getTrialBalance();

    /**
     * Generate trial balance for a specific date range.
     */
    TrialBalanceResponse getTrialBalance(LocalDate from, LocalDate to);
}
