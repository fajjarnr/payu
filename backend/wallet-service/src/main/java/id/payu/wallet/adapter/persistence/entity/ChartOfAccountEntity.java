package id.payu.wallet.adapter.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for Chart of Accounts — GL account classification.
 */
@Entity
@Table(name = "chart_of_accounts", indexes = {
    @Index(name = "idx_coa_code", columnList = "code", unique = true),
    @Index(name = "idx_coa_account_type", columnList = "account_type"),
    @Index(name = "idx_coa_parent_id", columnList = "parent_id"),
    @Index(name = "idx_coa_category", columnList = "category")
})
public class ChartOfAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "level", nullable = false)
    private int level;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "normal_balance", nullable = false, length = 10)
    private String normalBalance;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "IDR";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ChartOfAccountEntity() {
    }

    public ChartOfAccountEntity(UUID id, String code, String name, String description,
                                String accountType, String category, UUID parentId,
                                int level, boolean active, String normalBalance,
                                String currency, LocalDateTime createdAt, LocalDateTime updatedAt) {
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

    public static ChartOfAccountEntityBuilder builder() {
        return new ChartOfAccountEntityBuilder();
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
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getNormalBalance() { return normalBalance; }
    public void setNormalBalance(String normalBalance) { this.normalBalance = normalBalance; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class ChartOfAccountEntityBuilder {
        private UUID id;
        private String code;
        private String name;
        private String description;
        private String accountType;
        private String category;
        private UUID parentId;
        private int level;
        private boolean active = true;
        private String normalBalance;
        private String currency = "IDR";
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        ChartOfAccountEntityBuilder() {}

        public ChartOfAccountEntityBuilder id(UUID id) { this.id = id; return this; }
        public ChartOfAccountEntityBuilder code(String code) { this.code = code; return this; }
        public ChartOfAccountEntityBuilder name(String name) { this.name = name; return this; }
        public ChartOfAccountEntityBuilder description(String description) { this.description = description; return this; }
        public ChartOfAccountEntityBuilder accountType(String accountType) { this.accountType = accountType; return this; }
        public ChartOfAccountEntityBuilder category(String category) { this.category = category; return this; }
        public ChartOfAccountEntityBuilder parentId(UUID parentId) { this.parentId = parentId; return this; }
        public ChartOfAccountEntityBuilder level(int level) { this.level = level; return this; }
        public ChartOfAccountEntityBuilder active(boolean active) { this.active = active; return this; }
        public ChartOfAccountEntityBuilder normalBalance(String normalBalance) { this.normalBalance = normalBalance; return this; }
        public ChartOfAccountEntityBuilder currency(String currency) { this.currency = currency; return this; }
        public ChartOfAccountEntityBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ChartOfAccountEntityBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ChartOfAccountEntity build() {
            return new ChartOfAccountEntity(id, code, name, description, accountType,
                    category, parentId, level, active, normalBalance, currency,
                    createdAt, updatedAt);
        }
    }
}
