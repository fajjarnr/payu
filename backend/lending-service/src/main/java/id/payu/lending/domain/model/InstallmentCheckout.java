package id.payu.lending.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an installment checkout — a purchase converted into an installment
 * loan via the PayLater facility. Bridges the gap between PayLater credit and
 * gateway-facing installment payment method.
 */
public class InstallmentCheckout {

    private UUID id;
    private UUID userId;
    private UUID payLaterId;
    private UUID loanId;
    private String partnerId;
    private String externalOrderId;
    private BigDecimal purchaseAmount;
    private String currency;
    private int tenor;
    private BigDecimal monthlyPayment;
    private BigDecimal interestRate;
    private CheckoutStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum CheckoutStatus {
        PENDING,
        APPROVED,
        DISBURSED,
        REJECTED,
        CANCELLED,
        EXPIRED
    }

    public InstallmentCheckout() {}

    // Domain methods

    public void approve(UUID loanId) {
        this.status = CheckoutStatus.APPROVED;
        this.loanId = loanId;
        this.updatedAt = LocalDateTime.now();
    }

    public void markDisbursed() {
        this.status = CheckoutStatus.DISBURSED;
        this.updatedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        this.status = CheckoutStatus.REJECTED;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

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

    public UUID getPayLaterId() {
        return payLaterId;
    }

    public void setPayLaterId(UUID payLaterId) {
        this.payLaterId = payLaterId;
    }

    public UUID getLoanId() {
        return loanId;
    }

    public void setLoanId(UUID loanId) {
        this.loanId = loanId;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public void setExternalOrderId(String externalOrderId) {
        this.externalOrderId = externalOrderId;
    }

    public BigDecimal getPurchaseAmount() {
        return purchaseAmount;
    }

    public void setPurchaseAmount(BigDecimal purchaseAmount) {
        this.purchaseAmount = purchaseAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public int getTenor() {
        return tenor;
    }

    public void setTenor(int tenor) {
        this.tenor = tenor;
    }

    public BigDecimal getMonthlyPayment() {
        return monthlyPayment;
    }

    public void setMonthlyPayment(BigDecimal monthlyPayment) {
        this.monthlyPayment = monthlyPayment;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public CheckoutStatus getStatus() {
        return status;
    }

    public void setStatus(CheckoutStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
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
