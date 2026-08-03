package id.payu.lending.entity;

import id.payu.lending.domain.model.RepaymentPayment;
import id.payu.lending.domain.model.RepaymentPaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_repayment_payments")
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class RepaymentPaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "repayment_schedule_id", nullable = false)
    private UUID repaymentScheduleId;

    @Column(name = "loan_id", nullable = false)
    private UUID loanId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RepaymentPaymentStatus status;

    @Column(name = "wallet_transaction_id", length = 128)
    private String walletTransactionId;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Version
    private Long version;

    public RepaymentPaymentEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRepaymentScheduleId() { return repaymentScheduleId; }
    public void setRepaymentScheduleId(UUID value) { this.repaymentScheduleId = value; }
    public UUID getLoanId() { return loanId; }
    public void setLoanId(UUID value) { this.loanId = value; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID value) { this.userId = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { this.amount = value; }
    public String getCurrency() { return currency; }
    public void setCurrency(String value) { this.currency = value; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String value) { this.idempotencyKey = value; }
    public RepaymentPaymentStatus getStatus() { return status; }
    public void setStatus(RepaymentPaymentStatus value) { this.status = value; }
    public String getWalletTransactionId() { return walletTransactionId; }
    public void setWalletTransactionId(String value) { this.walletTransactionId = value; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String value) { this.failureReason = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { this.updatedAt = value; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String value) { this.tenantId = value; }
}
