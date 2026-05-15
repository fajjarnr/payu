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

public class QrPaymentRequest {

    @NotNull(message = "Sender account ID is required")
    private UUID senderAccountId;

    @NotBlank(message = "QR data is required")
    private String qrData;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Sensitive
    private BigDecimal amount;

    @Size(max = 140, message = "Memo must not exceed 140 characters")
    private String memo;

    @Size(min = 6, max = 6, message = "TransactionEntity PIN must be exactly 6 digits")
    @Pattern(regexp = "^\\d{6}$", message = "TransactionEntity PIN must be exactly 6 digits")
    @Sensitive(value = SensitivityLevel.CRITICAL)
    private String transactionPin;

    @Size(max = 100, message = "Idempotency key is too long")
    private String idempotencyKey;

    public QrPaymentRequest() {
    }

    public QrPaymentRequest(UUID senderAccountId, String qrData, BigDecimal amount, String memo, String transactionPin, String idempotencyKey) {
        this.senderAccountId = senderAccountId;
        this.qrData = qrData;
        this.amount = amount;
        this.memo = memo;
        this.transactionPin = transactionPin;
        this.idempotencyKey = idempotencyKey;
    }

    public static QrPaymentRequestBuilder builder() {
        return new QrPaymentRequestBuilder();
    }

    public UUID getSenderAccountId() {
        return senderAccountId;
    }

    public void setSenderAccountId(UUID senderAccountId) {
        this.senderAccountId = senderAccountId;
    }

    public String getQrData() {
        return qrData;
    }

    public void setQrData(String qrData) {
        this.qrData = qrData;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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

    public static class QrPaymentRequestBuilder {
        private UUID senderAccountId;
        private String qrData;
        private BigDecimal amount;
        private String memo;
        private String transactionPin;
        private String idempotencyKey;

        public QrPaymentRequestBuilder senderAccountId(UUID senderAccountId) {
            this.senderAccountId = senderAccountId;
            return this;
        }

        public QrPaymentRequestBuilder qrData(String qrData) {
            this.qrData = qrData;
            return this;
        }

        public QrPaymentRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public QrPaymentRequestBuilder memo(String memo) {
            this.memo = memo;
            return this;
        }

        public QrPaymentRequestBuilder transactionPin(String transactionPin) {
            this.transactionPin = transactionPin;
            return this;
        }

        public QrPaymentRequestBuilder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public QrPaymentRequest build() {
            return new QrPaymentRequest(senderAccountId, qrData, amount, memo, transactionPin, idempotencyKey);
        }
    }
}
