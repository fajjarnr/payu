package id.payu.transaction.interfaces.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ProcessQrisPaymentRequest {
    @NotBlank(message = "QRIS code is required")
    private String qrisCode;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String currency;

    @NotNull(message = "Customer account ID is required")
    private Long customerId;

    /** The UUID of the user's wallet account to debit for this QRIS payment. */
    @NotNull(message = "Account ID is required")
    private java.util.UUID accountId;

    private String transactionPin;
    private String deviceId;

    public ProcessQrisPaymentRequest() {
    }

    public String getQrisCode() {
        return qrisCode;
    }

    public void setQrisCode(String qrisCode) {
        this.qrisCode = qrisCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public java.util.UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(java.util.UUID accountId) {
        this.accountId = accountId;
    }

    public String getTransactionPin() {
        return transactionPin;
    }

    public void setTransactionPin(String transactionPin) {
        this.transactionPin = transactionPin;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
