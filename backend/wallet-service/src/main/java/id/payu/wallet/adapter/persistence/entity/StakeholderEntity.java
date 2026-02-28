package id.payu.wallet.adapter.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for revenue split stakeholders.
 */
@Entity
@Table(name = "revenue_split_stakeholders", indexes = {
    @Index(name = "idx_stakeholder_split", columnList = "revenue_split_id"),
    @Index(name = "idx_stakeholder_account", columnList = "account_id")
})
public class StakeholderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revenue_split_id", nullable = false)
    private RevenueSplitEntity revenueSplit;

    @Column(name = "account_id", nullable = false, length = 128)
    private String accountId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(precision = 8, scale = 4)
    private BigDecimal percentage;

    @Column(name = "fixed_amount", precision = 19, scale = 4)
    private BigDecimal fixedAmount;

    @Column(nullable = false)
    private int priority;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public StakeholderEntity() {
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public RevenueSplitEntity getRevenueSplit() { return revenueSplit; }
    public void setRevenueSplit(RevenueSplitEntity revenueSplit) { this.revenueSplit = revenueSplit; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    public BigDecimal getFixedAmount() { return fixedAmount; }
    public void setFixedAmount(BigDecimal fixedAmount) { this.fixedAmount = fixedAmount; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
