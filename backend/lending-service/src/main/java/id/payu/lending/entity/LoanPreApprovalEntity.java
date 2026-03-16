package id.payu.lending.entity;

import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_pre_approvals")
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class LoanPreApprovalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false)
    private id.payu.lending.domain.model.Loan.LoanType loanType;

    @Column(name = "requested_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "max_approved_amount", precision = 19, scale = 2)
    private BigDecimal maxApprovedAmount;

    @Column(name = "min_interest_rate", precision = 5, scale = 4)
    private BigDecimal minInterestRate;

    @Column(name = "max_tenure_months")
    private Integer maxTenureMonths;

    @Column(name = "estimated_monthly_payment", precision = 19, scale = 2)
    private BigDecimal estimatedMonthlyPayment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private id.payu.lending.domain.model.LoanPreApproval.PreApprovalStatus status;

    @Column(name = "credit_score", precision = 5, scale = 2)
    private BigDecimal creditScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category")
    private id.payu.lending.domain.model.CreditScore.RiskCategory riskCategory;

    @Column(name = "reason")
    private String reason;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public LoanPreApprovalEntity() {}

    public LoanPreApprovalEntity(UUID id, UUID userId, id.payu.lending.domain.model.Loan.LoanType loanType, 
                                BigDecimal requestedAmount, BigDecimal maxApprovedAmount, BigDecimal minInterestRate,
                                Integer maxTenureMonths, BigDecimal estimatedMonthlyPayment,
                                id.payu.lending.domain.model.LoanPreApproval.PreApprovalStatus status,
                                BigDecimal creditScore, id.payu.lending.domain.model.CreditScore.RiskCategory riskCategory,
                                String reason, LocalDate validUntil, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.loanType = loanType;
        this.requestedAmount = requestedAmount;
        this.maxApprovedAmount = maxApprovedAmount;
        this.minInterestRate = minInterestRate;
        this.maxTenureMonths = maxTenureMonths;
        this.estimatedMonthlyPayment = estimatedMonthlyPayment;
        this.status = status;
        this.creditScore = creditScore;
        this.riskCategory = riskCategory;
        this.reason = reason;
        this.validUntil = validUntil;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public id.payu.lending.domain.model.Loan.LoanType getLoanType() { return loanType; }
    public void setLoanType(id.payu.lending.domain.model.Loan.LoanType loanType) { this.loanType = loanType; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; }
    public BigDecimal getMaxApprovedAmount() { return maxApprovedAmount; }
    public void setMaxApprovedAmount(BigDecimal maxApprovedAmount) { this.maxApprovedAmount = maxApprovedAmount; }
    public BigDecimal getMinInterestRate() { return minInterestRate; }
    public void setMinInterestRate(BigDecimal minInterestRate) { this.minInterestRate = minInterestRate; }
    public Integer getMaxTenureMonths() { return maxTenureMonths; }
    public void setMaxTenureMonths(Integer maxTenureMonths) { this.maxTenureMonths = maxTenureMonths; }
    public BigDecimal getEstimatedMonthlyPayment() { return estimatedMonthlyPayment; }
    public void setEstimatedMonthlyPayment(BigDecimal estimatedMonthlyPayment) { this.estimatedMonthlyPayment = estimatedMonthlyPayment; }
    public id.payu.lending.domain.model.LoanPreApproval.PreApprovalStatus getStatus() { return status; }
    public void setStatus(id.payu.lending.domain.model.LoanPreApproval.PreApprovalStatus status) { this.status = status; }
    public BigDecimal getCreditScore() { return creditScore; }
    public void setCreditScore(BigDecimal creditScore) { this.creditScore = creditScore; }
    public id.payu.lending.domain.model.CreditScore.RiskCategory getRiskCategory() { return riskCategory; }
    public void setRiskCategory(id.payu.lending.domain.model.CreditScore.RiskCategory riskCategory) { this.riskCategory = riskCategory; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private UUID userId;
        private id.payu.lending.domain.model.Loan.LoanType loanType;
        private BigDecimal requestedAmount;
        private BigDecimal maxApprovedAmount;
        private BigDecimal minInterestRate;
        private Integer maxTenureMonths;
        private BigDecimal estimatedMonthlyPayment;
        private id.payu.lending.domain.model.LoanPreApproval.PreApprovalStatus status;
        private BigDecimal creditScore;
        private id.payu.lending.domain.model.CreditScore.RiskCategory riskCategory;
        private String reason;
        private LocalDate validUntil;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder loanType(id.payu.lending.domain.model.Loan.LoanType loanType) { this.loanType = loanType; return this; }
        public Builder requestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; return this; }
        public Builder maxApprovedAmount(BigDecimal maxApprovedAmount) { this.maxApprovedAmount = maxApprovedAmount; return this; }
        public Builder minInterestRate(BigDecimal minInterestRate) { this.minInterestRate = minInterestRate; return this; }
        public Builder maxTenureMonths(Integer maxTenureMonths) { this.maxTenureMonths = maxTenureMonths; return this; }
        public Builder estimatedMonthlyPayment(BigDecimal estimatedMonthlyPayment) { this.estimatedMonthlyPayment = estimatedMonthlyPayment; return this; }
        public Builder status(id.payu.lending.domain.model.LoanPreApproval.PreApprovalStatus status) { this.status = status; return this; }
        public Builder creditScore(BigDecimal creditScore) { this.creditScore = creditScore; return this; }
        public Builder riskCategory(id.payu.lending.domain.model.CreditScore.RiskCategory riskCategory) { this.riskCategory = riskCategory; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder validUntil(LocalDate validUntil) { this.validUntil = validUntil; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public LoanPreApprovalEntity build() {
            return new LoanPreApprovalEntity(id, userId, loanType, requestedAmount, maxApprovedAmount, 
                                            minInterestRate, maxTenureMonths, estimatedMonthlyPayment,
                                            status, creditScore, riskCategory, reason, validUntil, 
                                            createdAt, updatedAt);
        }
    }
}
