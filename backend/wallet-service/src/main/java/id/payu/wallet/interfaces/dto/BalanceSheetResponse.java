package id.payu.wallet.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Balance Sheet (Neraca) report — Assets = Liabilities + Equity.
 */
public class BalanceSheetResponse {

    private LocalDate asOfDate;
    private List<BalanceSheetEntry> assets;
    private List<BalanceSheetEntry> liabilities;
    private List<BalanceSheetEntry> equity;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal totalEquity;
    private boolean balanced;
    private LocalDateTime generatedAt;

    public BalanceSheetResponse() {
        this.assets = new ArrayList<>();
        this.liabilities = new ArrayList<>();
        this.equity = new ArrayList<>();
    }

    public BalanceSheetResponse(LocalDate asOfDate,
                                 List<BalanceSheetEntry> assets,
                                 List<BalanceSheetEntry> liabilities,
                                 List<BalanceSheetEntry> equity,
                                 BigDecimal totalAssets,
                                 BigDecimal totalLiabilities,
                                 BigDecimal totalEquity,
                                 boolean balanced,
                                 LocalDateTime generatedAt) {
        this.asOfDate = asOfDate;
        this.assets = assets;
        this.liabilities = liabilities;
        this.equity = equity;
        this.totalAssets = totalAssets;
        this.totalLiabilities = totalLiabilities;
        this.totalEquity = totalEquity;
        this.balanced = balanced;
        this.generatedAt = generatedAt;
    }

    public static BalanceSheetResponseBuilder builder() {
        return new BalanceSheetResponseBuilder();
    }

    // Getters and Setters
    public LocalDate getAsOfDate() { return asOfDate; }
    public void setAsOfDate(LocalDate asOfDate) { this.asOfDate = asOfDate; }
    public List<BalanceSheetEntry> getAssets() { return assets; }
    public void setAssets(List<BalanceSheetEntry> assets) { this.assets = assets; }
    public List<BalanceSheetEntry> getLiabilities() { return liabilities; }
    public void setLiabilities(List<BalanceSheetEntry> liabilities) { this.liabilities = liabilities; }
    public List<BalanceSheetEntry> getEquity() { return equity; }
    public void setEquity(List<BalanceSheetEntry> equity) { this.equity = equity; }
    public BigDecimal getTotalAssets() { return totalAssets; }
    public void setTotalAssets(BigDecimal totalAssets) { this.totalAssets = totalAssets; }
    public BigDecimal getTotalLiabilities() { return totalLiabilities; }
    public void setTotalLiabilities(BigDecimal totalLiabilities) { this.totalLiabilities = totalLiabilities; }
    public BigDecimal getTotalEquity() { return totalEquity; }
    public void setTotalEquity(BigDecimal totalEquity) { this.totalEquity = totalEquity; }
    public boolean isBalanced() { return balanced; }
    public void setBalanced(boolean balanced) { this.balanced = balanced; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public static class BalanceSheetEntry {
        private String coaCode;
        private String accountName;
        private String category;
        private BigDecimal balance;

        public BalanceSheetEntry() {}

        public BalanceSheetEntry(String coaCode, String accountName, String category, BigDecimal balance) {
            this.coaCode = coaCode;
            this.accountName = accountName;
            this.category = category;
            this.balance = balance;
        }

        public String getCoaCode() { return coaCode; }
        public void setCoaCode(String coaCode) { this.coaCode = coaCode; }
        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public BigDecimal getBalance() { return balance; }
        public void setBalance(BigDecimal balance) { this.balance = balance; }
    }

    public static class BalanceSheetResponseBuilder {
        private LocalDate asOfDate;
        private List<BalanceSheetEntry> assets = new ArrayList<>();
        private List<BalanceSheetEntry> liabilities = new ArrayList<>();
        private List<BalanceSheetEntry> equity = new ArrayList<>();
        private BigDecimal totalAssets;
        private BigDecimal totalLiabilities;
        private BigDecimal totalEquity;
        private boolean balanced;
        private LocalDateTime generatedAt;

        BalanceSheetResponseBuilder() {}

        public BalanceSheetResponseBuilder asOfDate(LocalDate asOfDate) { this.asOfDate = asOfDate; return this; }
        public BalanceSheetResponseBuilder assets(List<BalanceSheetEntry> assets) { this.assets = assets; return this; }
        public BalanceSheetResponseBuilder liabilities(List<BalanceSheetEntry> liabilities) { this.liabilities = liabilities; return this; }
        public BalanceSheetResponseBuilder equity(List<BalanceSheetEntry> equity) { this.equity = equity; return this; }
        public BalanceSheetResponseBuilder totalAssets(BigDecimal totalAssets) { this.totalAssets = totalAssets; return this; }
        public BalanceSheetResponseBuilder totalLiabilities(BigDecimal totalLiabilities) { this.totalLiabilities = totalLiabilities; return this; }
        public BalanceSheetResponseBuilder totalEquity(BigDecimal totalEquity) { this.totalEquity = totalEquity; return this; }
        public BalanceSheetResponseBuilder balanced(boolean balanced) { this.balanced = balanced; return this; }
        public BalanceSheetResponseBuilder generatedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; return this; }

        public BalanceSheetResponse build() {
            return new BalanceSheetResponse(asOfDate, assets, liabilities, equity,
                    totalAssets, totalLiabilities, totalEquity, balanced, generatedAt);
        }
    }
}
