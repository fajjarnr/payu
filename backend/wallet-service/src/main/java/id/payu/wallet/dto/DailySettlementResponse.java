package id.payu.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Daily Settlement Report for partner reconciliation (e.g., TokoBapak).
 */
public class DailySettlementResponse {

    private LocalDate settlementDate;
    private int totalTransactions;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private BigDecimal netSettlement;
    private List<SettlementEntry> entries;
    private LocalDateTime generatedAt;

    public DailySettlementResponse() {
        this.entries = new ArrayList<>();
    }

    public DailySettlementResponse(LocalDate settlementDate, int totalTransactions,
                                    BigDecimal totalDebits, BigDecimal totalCredits,
                                    BigDecimal netSettlement, List<SettlementEntry> entries,
                                    LocalDateTime generatedAt) {
        this.settlementDate = settlementDate;
        this.totalTransactions = totalTransactions;
        this.totalDebits = totalDebits;
        this.totalCredits = totalCredits;
        this.netSettlement = netSettlement;
        this.entries = entries;
        this.generatedAt = generatedAt;
    }

    public static DailySettlementResponseBuilder builder() {
        return new DailySettlementResponseBuilder();
    }

    // Getters and Setters
    public LocalDate getSettlementDate() { return settlementDate; }
    public void setSettlementDate(LocalDate settlementDate) { this.settlementDate = settlementDate; }
    public int getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(int totalTransactions) { this.totalTransactions = totalTransactions; }
    public BigDecimal getTotalDebits() { return totalDebits; }
    public void setTotalDebits(BigDecimal totalDebits) { this.totalDebits = totalDebits; }
    public BigDecimal getTotalCredits() { return totalCredits; }
    public void setTotalCredits(BigDecimal totalCredits) { this.totalCredits = totalCredits; }
    public BigDecimal getNetSettlement() { return netSettlement; }
    public void setNetSettlement(BigDecimal netSettlement) { this.netSettlement = netSettlement; }
    public List<SettlementEntry> getEntries() { return entries; }
    public void setEntries(List<SettlementEntry> entries) { this.entries = entries; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public static class SettlementEntry {
        private String journalNumber;
        private String referenceType;
        private String referenceId;
        private String debitAccount;
        private String creditAccount;
        private BigDecimal amount;
        private LocalDateTime postedAt;

        public SettlementEntry() {}

        public SettlementEntry(String journalNumber, String referenceType, String referenceId,
                               String debitAccount, String creditAccount, BigDecimal amount,
                               LocalDateTime postedAt) {
            this.journalNumber = journalNumber;
            this.referenceType = referenceType;
            this.referenceId = referenceId;
            this.debitAccount = debitAccount;
            this.creditAccount = creditAccount;
            this.amount = amount;
            this.postedAt = postedAt;
        }

        public String getJournalNumber() { return journalNumber; }
        public void setJournalNumber(String journalNumber) { this.journalNumber = journalNumber; }
        public String getReferenceType() { return referenceType; }
        public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
        public String getReferenceId() { return referenceId; }
        public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
        public String getDebitAccount() { return debitAccount; }
        public void setDebitAccount(String debitAccount) { this.debitAccount = debitAccount; }
        public String getCreditAccount() { return creditAccount; }
        public void setCreditAccount(String creditAccount) { this.creditAccount = creditAccount; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public LocalDateTime getPostedAt() { return postedAt; }
        public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }
    }

    public static class DailySettlementResponseBuilder {
        private LocalDate settlementDate;
        private int totalTransactions;
        private BigDecimal totalDebits;
        private BigDecimal totalCredits;
        private BigDecimal netSettlement;
        private List<SettlementEntry> entries = new ArrayList<>();
        private LocalDateTime generatedAt;

        DailySettlementResponseBuilder() {}

        public DailySettlementResponseBuilder settlementDate(LocalDate settlementDate) { this.settlementDate = settlementDate; return this; }
        public DailySettlementResponseBuilder totalTransactions(int totalTransactions) { this.totalTransactions = totalTransactions; return this; }
        public DailySettlementResponseBuilder totalDebits(BigDecimal totalDebits) { this.totalDebits = totalDebits; return this; }
        public DailySettlementResponseBuilder totalCredits(BigDecimal totalCredits) { this.totalCredits = totalCredits; return this; }
        public DailySettlementResponseBuilder netSettlement(BigDecimal netSettlement) { this.netSettlement = netSettlement; return this; }
        public DailySettlementResponseBuilder entries(List<SettlementEntry> entries) { this.entries = entries; return this; }
        public DailySettlementResponseBuilder generatedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; return this; }

        public DailySettlementResponse build() {
            return new DailySettlementResponse(settlementDate, totalTransactions, totalDebits,
                    totalCredits, netSettlement, entries, generatedAt);
        }
    }
}
