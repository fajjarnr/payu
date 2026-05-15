package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Chart of Accounts domain model for GL account classification.
 * Supports hierarchical code structure for banking:
 * - ASSET (1xxx): User wallets, bank accounts
 * - LIABILITY (2xxx): Escrow holdings, payables
 * - EQUITY (3xxx): Capital, retained earnings
 * - REVENUE (4xxx): Transaction fees, interest income
 * - EXPENSE (5xxx): Operational costs
 */
public class ChartOfAccount {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private AccountType accountType;
    private AccountCategory category;
    private UUID parentId;
    private int level;
    private boolean active;
    private NormalBalance normalBalance;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ChartOfAccount() {
    }

    public ChartOfAccount(UUID id, String code, String name, String description,
                          AccountType accountType, AccountCategory category,
                          UUID parentId, int level, boolean active,
                          NormalBalance normalBalance, String currency,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.accountType = accountType;
        this.category = category;
        this.parentId = parentId;
        this.level = level;
        this.active = active;
        this.normalBalance = normalBalance;
        this.currency = currency;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Top-level account classification per PSAK (Indonesian Accounting Standards).
     */

    /**
     * Sub-categories for more granular GL classification.
     */

    /**
     * Normal balance side for accounting equation.
     */

    /**
     * Determines if this is a balance sheet account (ASSET, LIABILITY, EQUITY).
     */
    public boolean isBalanceSheetAccount() {
        return accountType == AccountType.ASSET
                || accountType == AccountType.LIABILITY
                || accountType == AccountType.EQUITY;
    }

    /**
     * Determines if this is an income statement account (REVENUE, EXPENSE).
     */
    public boolean isIncomeStatementAccount() {
        return accountType == AccountType.REVENUE
                || accountType == AccountType.EXPENSE;
    }

    public static ChartOfAccountBuilder builder() {
        return new ChartOfAccountBuilder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }
    public AccountCategory getCategory() { return category; }
    public void setCategory(AccountCategory category) { this.category = category; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public NormalBalance getNormalBalance() { return normalBalance; }
    public void setNormalBalance(NormalBalance normalBalance) { this.normalBalance = normalBalance; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class ChartOfAccountBuilder {
        private UUID id;
        private String code;
        private String name;
        private String description;
        private AccountType accountType;
        private AccountCategory category;
        private UUID parentId;
        private int level;
        private boolean active = true;
        private NormalBalance normalBalance;
        private String currency = "IDR";
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        ChartOfAccountBuilder() {}

        public ChartOfAccountBuilder id(UUID id) { this.id = id; return this; }
        public ChartOfAccountBuilder code(String code) { this.code = code; return this; }
        public ChartOfAccountBuilder name(String name) { this.name = name; return this; }
        public ChartOfAccountBuilder description(String description) { this.description = description; return this; }
        public ChartOfAccountBuilder accountType(AccountType accountType) { this.accountType = accountType; return this; }
        public ChartOfAccountBuilder category(AccountCategory category) { this.category = category; return this; }
        public ChartOfAccountBuilder parentId(UUID parentId) { this.parentId = parentId; return this; }
        public ChartOfAccountBuilder level(int level) { this.level = level; return this; }
        public ChartOfAccountBuilder active(boolean active) { this.active = active; return this; }
        public ChartOfAccountBuilder normalBalance(NormalBalance normalBalance) { this.normalBalance = normalBalance; return this; }
        public ChartOfAccountBuilder currency(String currency) { this.currency = currency; return this; }
        public ChartOfAccountBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ChartOfAccountBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ChartOfAccount build() {
            return new ChartOfAccount(id, code, name, description, accountType,
                    category, parentId, level, active, normalBalance, currency,
                    createdAt, updatedAt);
        }
    }
}
