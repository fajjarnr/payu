package id.payu.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Income Statement (Laba-Rugi) report — Revenue - Expenses = Net Income.
 */
public class IncomeStatementResponse {

    private LocalDate periodFrom;
    private LocalDate periodTo;
    private List<IncomeStatementEntry> revenues;
    private List<IncomeStatementEntry> expenses;
    private BigDecimal totalRevenue;
    private BigDecimal totalExpenses;
    private BigDecimal netIncome;
    private LocalDateTime generatedAt;

    public IncomeStatementResponse() {
        this.revenues = new ArrayList<>();
        this.expenses = new ArrayList<>();
    }

    public IncomeStatementResponse(LocalDate periodFrom, LocalDate periodTo,
                                    List<IncomeStatementEntry> revenues,
                                    List<IncomeStatementEntry> expenses,
                                    BigDecimal totalRevenue, BigDecimal totalExpenses,
                                    BigDecimal netIncome, LocalDateTime generatedAt) {
        this.periodFrom = periodFrom;
        this.periodTo = periodTo;
        this.revenues = revenues;
        this.expenses = expenses;
        this.totalRevenue = totalRevenue;
        this.totalExpenses = totalExpenses;
        this.netIncome = netIncome;
        this.generatedAt = generatedAt;
    }

    public static IncomeStatementResponseBuilder builder() {
        return new IncomeStatementResponseBuilder();
    }

    // Getters and Setters
    public LocalDate getPeriodFrom() { return periodFrom; }
    public void setPeriodFrom(LocalDate periodFrom) { this.periodFrom = periodFrom; }
    public LocalDate getPeriodTo() { return periodTo; }
    public void setPeriodTo(LocalDate periodTo) { this.periodTo = periodTo; }
    public List<IncomeStatementEntry> getRevenues() { return revenues; }
    public void setRevenues(List<IncomeStatementEntry> revenues) { this.revenues = revenues; }
    public List<IncomeStatementEntry> getExpenses() { return expenses; }
    public void setExpenses(List<IncomeStatementEntry> expenses) { this.expenses = expenses; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    public BigDecimal getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(BigDecimal totalExpenses) { this.totalExpenses = totalExpenses; }
    public BigDecimal getNetIncome() { return netIncome; }
    public void setNetIncome(BigDecimal netIncome) { this.netIncome = netIncome; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public static class IncomeStatementEntry {
        private String coaCode;
        private String accountName;
        private String category;
        private BigDecimal amount;

        public IncomeStatementEntry() {}

        public IncomeStatementEntry(String coaCode, String accountName, String category, BigDecimal amount) {
            this.coaCode = coaCode;
            this.accountName = accountName;
            this.category = category;
            this.amount = amount;
        }

        public String getCoaCode() { return coaCode; }
        public void setCoaCode(String coaCode) { this.coaCode = coaCode; }
        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    public static class IncomeStatementResponseBuilder {
        private LocalDate periodFrom;
        private LocalDate periodTo;
        private List<IncomeStatementEntry> revenues = new ArrayList<>();
        private List<IncomeStatementEntry> expenses = new ArrayList<>();
        private BigDecimal totalRevenue;
        private BigDecimal totalExpenses;
        private BigDecimal netIncome;
        private LocalDateTime generatedAt;

        IncomeStatementResponseBuilder() {}

        public IncomeStatementResponseBuilder periodFrom(LocalDate periodFrom) { this.periodFrom = periodFrom; return this; }
        public IncomeStatementResponseBuilder periodTo(LocalDate periodTo) { this.periodTo = periodTo; return this; }
        public IncomeStatementResponseBuilder revenues(List<IncomeStatementEntry> revenues) { this.revenues = revenues; return this; }
        public IncomeStatementResponseBuilder expenses(List<IncomeStatementEntry> expenses) { this.expenses = expenses; return this; }
        public IncomeStatementResponseBuilder totalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; return this; }
        public IncomeStatementResponseBuilder totalExpenses(BigDecimal totalExpenses) { this.totalExpenses = totalExpenses; return this; }
        public IncomeStatementResponseBuilder netIncome(BigDecimal netIncome) { this.netIncome = netIncome; return this; }
        public IncomeStatementResponseBuilder generatedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; return this; }

        public IncomeStatementResponse build() {
            return new IncomeStatementResponse(periodFrom, periodTo, revenues, expenses,
                    totalRevenue, totalExpenses, netIncome, generatedAt);
        }
    }
}
