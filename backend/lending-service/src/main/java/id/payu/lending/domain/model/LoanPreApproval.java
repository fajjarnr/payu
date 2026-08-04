package id.payu.lending.domain.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import id.payu.lending.dto.LoanPreApprovalResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class LoanPreApproval {

    private UUID id;
    private UUID userId;
    private LoanType loanType;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal requestedAmount;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal maxApprovedAmount;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal minInterestRate;
    private Integer maxTenureMonths;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal estimatedMonthlyPayment;
    private PreApprovalStatus status;
    private BigDecimal creditScore;
    private RiskCategory riskCategory;
    private String reason;
    private LocalDate validUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LoanPreApproval() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public LoanType getLoanType() {
        return loanType;
    }

    public void setLoanType(LoanType loanType) {
        this.loanType = loanType;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public BigDecimal getMaxApprovedAmount() {
        return maxApprovedAmount;
    }

    public void setMaxApprovedAmount(BigDecimal maxApprovedAmount) {
        this.maxApprovedAmount = maxApprovedAmount;
    }

    public BigDecimal getMinInterestRate() {
        return minInterestRate;
    }

    public void setMinInterestRate(BigDecimal minInterestRate) {
        this.minInterestRate = minInterestRate;
    }

    public Integer getMaxTenureMonths() {
        return maxTenureMonths;
    }

    public void setMaxTenureMonths(Integer maxTenureMonths) {
        this.maxTenureMonths = maxTenureMonths;
    }

    public BigDecimal getEstimatedMonthlyPayment() {
        return estimatedMonthlyPayment;
    }

    public void setEstimatedMonthlyPayment(BigDecimal estimatedMonthlyPayment) {
        this.estimatedMonthlyPayment = estimatedMonthlyPayment;
    }

    public PreApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(PreApprovalStatus status) {
        this.status = status;
    }

    public BigDecimal getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(BigDecimal creditScore) {
        this.creditScore = creditScore;
    }

    public RiskCategory getRiskCategory() {
        return riskCategory;
    }

    public void setRiskCategory(RiskCategory riskCategory) {
        this.riskCategory = riskCategory;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
