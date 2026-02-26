package id.payu.wallet.adapter.persistence.entity;

import id.payu.wallet.multitenancy.TenantAware;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "split_payment_executions", indexes = {
    @Index(name = "idx_split_exec_payer", columnList = "payer_account_id"),
    @Index(name = "idx_split_exec_partner", columnList = "partner_id"),
    @Index(name = "idx_split_exec_status", columnList = "status"),
    @Index(name = "idx_split_exec_idempotency", columnList = "idempotency_key"),
    @Index(name = "idx_split_exec_tenant", columnList = "tenant_id")
}, uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key"))
@TenantAware
public class SplitPaymentExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "split_rule_id")
    private UUID splitRuleId;

    @Column(name = "payer_account_id", nullable = false, length = 128)
    private String payerAccountId;

    @Column(name = "partner_id", length = 128)
    private String partnerId;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "external_reference_id", length = 128)
    private String externalReferenceId;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SplitExecutionStatus status;

    @Column(length = 512)
    private String description;

    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SplitPaymentLegEntity> legs = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    public enum SplitExecutionStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, REVERSED
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSplitRuleId() { return splitRuleId; }
    public void setSplitRuleId(UUID splitRuleId) { this.splitRuleId = splitRuleId; }
    public String getPayerAccountId() { return payerAccountId; }
    public void setPayerAccountId(String payerAccountId) { this.payerAccountId = payerAccountId; }
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getExternalReferenceId() { return externalReferenceId; }
    public void setExternalReferenceId(String externalReferenceId) { this.externalReferenceId = externalReferenceId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public SplitExecutionStatus getStatus() { return status; }
    public void setStatus(SplitExecutionStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public List<SplitPaymentLegEntity> getLegs() { return legs; }
    public void setLegs(List<SplitPaymentLegEntity> legs) { this.legs = legs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getFailedAt() { return failedAt; }
    public void setFailedAt(LocalDateTime failedAt) { this.failedAt = failedAt; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
