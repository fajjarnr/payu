package id.payu.transaction.dto;

import id.payu.security.annotation.Sensitive;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;
import id.payu.security.annotation.SensitivityLevel;

public class P2PTransferRequest {

    @NotNull(message = "Sender account ID is required")
    private UUID senderAccountId;

    @NotBlank(message = "Destination phone number is required")
    @Pattern(regexp = "^08[0-9]{8,12}$", message = "Invalid Indonesian phone number format")
    private String destinationPhone;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Sensitive
    private BigDecimal amount;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code")
    private String currency;

    @Size(max = 140, message = "Memo must not exceed 140 characters")
    private String memo;

    @Size(min = 6, max = 6, message = "TransactionEntity PIN must be exactly 6 digits")
    @Pattern(regexp = "^\\d{6}$", message = "TransactionEntity PIN must be exactly 6 digits")
    @Sensitive(value = SensitivityLevel.CRITICAL)
    private String transactionPin;

    @Size(max = 100, message = "Idempotency key is too long")
    private String idempotencyKey;

    public P2PTransferRequest() {
    }

    public P2PTransferRequest(UUID senderAccountId, String destinationPhone, BigDecimal amount, String currency,
                              String memo, String transactionPin, String idempotencyKey) {
        this.senderAccountId = senderAccountId;
        this.destinationPhone = destinationPhone;
        this.amount = amount;
        this.currency = currency;
        this.memo = memo;
        this.transactionPin = transactionPin;
        this.idempotencyKey = idempotencyKey;
    }

    public static P2PTransferRequestBuilder builder() {
        return new P2PTransferRequestBuilder();
    }

    public UUID getSenderAccountId() {
        return senderAccountId;
    }

    public void setSenderAccountId(UUID senderAccountId) {
        this.senderAccountId = senderAccountId;
    }

    public String getDestinationPhone() {
        return destinationPhone;
    }

    public void setDestinationPhone(String destinationPhone) {
        this.destinationPhone = destinationPhone;
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

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getTransactionPin() {
        return transactionPin;
    }

    public void setTransactionPin(String transactionPin) {
        this.transactionPin = transactionPin;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public static class P2PTransferRequestBuilder {
        private UUID senderAccountId;
        private String destinationPhone;
        private BigDecimal amount;
        private String currency;
        private String memo;
        private String transactionPin;
        private String idempotencyKey;

        public P2PTransferRequestBuilder senderAccountId(UUID senderAccountId) {
            this.senderAccountId = senderAccountId;
            return this;
        }

        public P2PTransferRequestBuilder destinationPhone(String destinationPhone) {
            this.destinationPhone = destinationPhone;
            return this;
        }

        public P2PTransferRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public P2PTransferRequestBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public P2PTransferRequestBuilder memo(String memo) {
            this.memo = memo;
            return this;
        }

        public P2PTransferRequestBuilder transactionPin(String transactionPin) {
            this.transactionPin = transactionPin;
            return this;
        }

        public P2PTransferRequestBuilder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public P2PTransferRequest build() {
            return new P2PTransferRequest(senderAccountId, destinationPhone, amount, currency, memo, transactionPin, idempotencyKey);
        }
    }
}
