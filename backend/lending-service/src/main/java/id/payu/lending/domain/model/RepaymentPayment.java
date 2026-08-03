package id.payu.lending.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class RepaymentPayment {

    private UUID id;
    private UUID repaymentScheduleId;
    private UUID loanId;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    private String idempotencyKey;
    private RepaymentPaymentStatus status;
    private String walletTransactionId;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRepaymentScheduleId() { return repaymentScheduleId; }
    public void setRepaymentScheduleId(UUID repaymentScheduleId) { this.repaymentScheduleId = repaymentScheduleId; }
    public UUID getLoanId() { return loanId; }
    public void setLoanId(UUID loanId) { this.loanId = loanId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public RepaymentPaymentStatus getStatus() { return status; }
    public void setStatus(RepaymentPaymentStatus status) { this.status = status; }
    public String getWalletTransactionId() { return walletTransactionId; }
    public void setWalletTransactionId(String walletTransactionId) { this.walletTransactionId = walletTransactionId; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
