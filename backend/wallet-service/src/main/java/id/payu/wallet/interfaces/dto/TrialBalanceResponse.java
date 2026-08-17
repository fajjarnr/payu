package id.payu.wallet.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Trial Balance report showing debit/credit totals per account.
 * Validates the accounting equation: total debits == total credits.
 */
public class TrialBalanceResponse {

    private LocalDate reportDate;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private List<TrialBalanceEntry> entries;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private boolean balanced;
    private LocalDateTime generatedAt;

    public TrialBalanceResponse() {
        this.entries = new ArrayList<>();
    }

    public TrialBalanceResponse(LocalDate reportDate, LocalDate periodFrom, LocalDate periodTo,
                                 List<TrialBalanceEntry> entries, BigDecimal totalDebits,
                                 BigDecimal totalCredits, boolean balanced, LocalDateTime generatedAt) {
        this.reportDate = reportDate;
        this.periodFrom = periodFrom;
        this.periodTo = periodTo;
        this.entries = entries;
        this.totalDebits = totalDebits;
        this.totalCredits = totalCredits;
        this.balanced = balanced;
        this.generatedAt = generatedAt;
    }

    public static TrialBalanceResponseBuilder builder() {
        return new TrialBalanceResponseBuilder();
    }

    // Getters and Setters
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public LocalDate getPeriodFrom() { return periodFrom; }
    public void setPeriodFrom(LocalDate periodFrom) { this.periodFrom = periodFrom; }
    public LocalDate getPeriodTo() { return periodTo; }
    public void setPeriodTo(LocalDate periodTo) { this.periodTo = periodTo; }
    public List<TrialBalanceEntry> getEntries() { return entries; }
    public void setEntries(List<TrialBalanceEntry> entries) { this.entries = entries; }
    public BigDecimal getTotalDebits() { return totalDebits; }
    public void setTotalDebits(BigDecimal totalDebits) { this.totalDebits = totalDebits; }
    public BigDecimal getTotalCredits() { return totalCredits; }
    public void setTotalCredits(BigDecimal totalCredits) { this.totalCredits = totalCredits; }
    public boolean isBalanced() { return balanced; }
    public void setBalanced(boolean balanced) { this.balanced = balanced; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    /**
     * A single row in the trial balance.
     */
    public static class TrialBalanceEntry {
        private String coaCode;
        private String accountName;
        private String accountType;
        private BigDecimal debitTotal;
        private BigDecimal creditTotal;
        private BigDecimal netBalance;

        public TrialBalanceEntry() {}

        public TrialBalanceEntry(String coaCode, String accountName, String accountType,
                                 BigDecimal debitTotal, BigDecimal creditTotal, BigDecimal netBalance) {
            this.coaCode = coaCode;
            this.accountName = accountName;
            this.accountType = accountType;
            this.debitTotal = debitTotal;
            this.creditTotal = creditTotal;
            this.netBalance = netBalance;
        }

        public String getCoaCode() { return coaCode; }
        public void setCoaCode(String coaCode) { this.coaCode = coaCode; }
        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }
        public String getAccountType() { return accountType; }
        public void setAccountType(String accountType) { this.accountType = accountType; }
        public BigDecimal getDebitTotal() { return debitTotal; }
        public void setDebitTotal(BigDecimal debitTotal) { this.debitTotal = debitTotal; }
        public BigDecimal getCreditTotal() { return creditTotal; }
        public void setCreditTotal(BigDecimal creditTotal) { this.creditTotal = creditTotal; }
        public BigDecimal getNetBalance() { return netBalance; }
        public void setNetBalance(BigDecimal netBalance) { this.netBalance = netBalance; }
    }

    public static class TrialBalanceResponseBuilder {
        private LocalDate reportDate;
        private LocalDate periodFrom;
        private LocalDate periodTo;
        private List<TrialBalanceEntry> entries = new ArrayList<>();
        private BigDecimal totalDebits;
        private BigDecimal totalCredits;
        private boolean balanced;
        private LocalDateTime generatedAt;

        TrialBalanceResponseBuilder() {}

        public TrialBalanceResponseBuilder reportDate(LocalDate reportDate) { this.reportDate = reportDate; return this; }
        public TrialBalanceResponseBuilder periodFrom(LocalDate periodFrom) { this.periodFrom = periodFrom; return this; }
        public TrialBalanceResponseBuilder periodTo(LocalDate periodTo) { this.periodTo = periodTo; return this; }
        public TrialBalanceResponseBuilder entries(List<TrialBalanceEntry> entries) { this.entries = entries; return this; }
        public TrialBalanceResponseBuilder totalDebits(BigDecimal totalDebits) { this.totalDebits = totalDebits; return this; }
        public TrialBalanceResponseBuilder totalCredits(BigDecimal totalCredits) { this.totalCredits = totalCredits; return this; }
        public TrialBalanceResponseBuilder balanced(boolean balanced) { this.balanced = balanced; return this; }
        public TrialBalanceResponseBuilder generatedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; return this; }

        public TrialBalanceResponse build() {
            return new TrialBalanceResponse(reportDate, periodFrom, periodTo, entries,
                    totalDebits, totalCredits, balanced, generatedAt);
        }
    }
}
