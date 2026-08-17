package id.payu.wallet.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request to create a journal entry with paired debit+credit entries.
 */
public class CreateJournalRequest {

    @NotBlank(message = "Description is required")
    private String description;

    private String referenceType;
    private String referenceId;

    @NotEmpty(message = "At least two entries (debit + credit) are required")
    @Valid
    private List<JournalLedgerEntryRequest> entries;

    public CreateJournalRequest() {}

    public CreateJournalRequest(String description, String referenceType, String referenceId,
                                 List<JournalLedgerEntryRequest> entries) {
        this.description = description;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.entries = entries;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public List<JournalLedgerEntryRequest> getEntries() { return entries; }
    public void setEntries(List<JournalLedgerEntryRequest> entries) { this.entries = entries; }

    /**
     * Individual ledger entry within a journal.
     */
    public static class JournalLedgerEntryRequest {

        @NotBlank(message = "Account ID is required")
        private String accountId;

        private String coaCode;

        @NotNull(message = "Entry type is required (DEBIT or CREDIT)")
        private String entryType;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private BigDecimal amount;

        private String currency;

        public JournalLedgerEntryRequest() {}

        public JournalLedgerEntryRequest(String accountId, String coaCode, String entryType,
                                          BigDecimal amount, String currency) {
            this.accountId = accountId;
            this.coaCode = coaCode;
            this.entryType = entryType;
            this.amount = amount;
            this.currency = currency;
        }

        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }
        public String getCoaCode() { return coaCode; }
        public void setCoaCode(String coaCode) { this.coaCode = coaCode; }
        public String getEntryType() { return entryType; }
        public void setEntryType(String entryType) { this.entryType = entryType; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }
}
