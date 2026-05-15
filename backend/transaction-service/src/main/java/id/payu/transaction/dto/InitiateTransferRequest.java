package id.payu.transaction.dto;

import id.payu.security.annotation.Sensitive;
import id.payu.security.annotation.SensitivityLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import id.payu.transaction.dto.TransactionType;

/**
 * Transfer request DTO with comprehensive input validation.
 *
 * Security considerations:
 * - Account numbers validated to prevent injection
 * - Amount constraints prevent overflow attacks
 * - Description length prevents DoS via large payloads
 * - PIN validation for security-critical operations
 * - Device ID for fraud detection
 */
public class InitiateTransferRequest {
    public InitiateTransferRequest() {
    }

    public InitiateTransferRequest(UUID senderAccountId, String recipientAccountNumber, BigDecimal amount, String currency, String description, TransactionType type, String transactionPin, String deviceId, String idempotencyKey, String memo) {
        this.senderAccountId = senderAccountId;
        this.recipientAccountNumber = recipientAccountNumber;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.type = type;
        this.transactionPin = transactionPin;
        this.deviceId = deviceId;
        this.idempotencyKey = idempotencyKey;
        this.memo = memo;
    }

    public static InitiateTransferRequestBuilder builder() {
        return new InitiateTransferRequestBuilder();
    }

    public static class InitiateTransferRequestBuilder {
        private UUID senderAccountId;
        private String recipientAccountNumber;
        private BigDecimal amount;
        private String currency;
        private String description;
        private TransactionType type;
        private String transactionPin;
        private String deviceId;
        private String idempotencyKey;
        private String memo;

        public InitiateTransferRequestBuilder senderAccountId(UUID senderAccountId) {
            this.senderAccountId = senderAccountId;
            return this;
        }
        public InitiateTransferRequestBuilder recipientAccountNumber(String recipientAccountNumber) {
            this.recipientAccountNumber = recipientAccountNumber;
            return this;
        }
        public InitiateTransferRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        public InitiateTransferRequestBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }
        public InitiateTransferRequestBuilder description(String description) {
            this.description = description;
            return this;
        }
        public InitiateTransferRequestBuilder type(TransactionType type) {
            this.type = type;
            return this;
        }
        public InitiateTransferRequestBuilder transactionPin(String transactionPin) {
            this.transactionPin = transactionPin;
            return this;
        }
        public InitiateTransferRequestBuilder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }
        public InitiateTransferRequestBuilder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }
        public InitiateTransferRequestBuilder memo(String memo) {
            this.memo = memo;
            return this;
        }

        public InitiateTransferRequest build() {
            return new InitiateTransferRequest(senderAccountId, recipientAccountNumber, amount, currency, description, type, transactionPin, deviceId, idempotencyKey, memo);
        }
    }

    public UUID getSenderAccountId() {
        return senderAccountId;
    }

    public void setSenderAccountId(UUID senderAccountId) {
        this.senderAccountId = senderAccountId;
    }

    public String getRecipientAccountNumber() {
        return recipientAccountNumber;
    }

    public void setRecipientAccountNumber(String recipientAccountNumber) {
        this.recipientAccountNumber = recipientAccountNumber;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }


    @NotNull(message = "Sender account ID is required")
    private UUID senderAccountId;

    @NotBlank(message = "Recipient account number is required")
    @Size(min = 10, max = 20, message = "Account number must be between 10 and 20 digits")
    @Pattern(regexp = "^[0-9]+$", message = "Account number must contain only digits")
    @Sensitive
    private String recipientAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Sensitive
    private BigDecimal amount;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code (e.g., IDR, USD)")
    private String currency;

    @NotBlank(message = "Description is required")
    @Size(min = 1, max = 100, message = "Description must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-.,#()]+$", message = "Description contains invalid characters")
    private String description;

    private TransactionType type;

    @Size(min = 6, max = 6, message = "TransactionEntity PIN must be exactly 6 digits")
    @Pattern(regexp = "^\\d{6}$", message = "TransactionEntity PIN must be exactly 6 digits")
    @Sensitive(value = SensitivityLevel.CRITICAL)
    private String transactionPin;

    @Size(max = 100, message = "Device ID is too long")
    private String deviceId;

    @Size(max = 100, message = "Idempotency key is too long")
    @Pattern(regexp = "^[a-zA-Z0-9\\-]+$", message = "Idempotency key contains invalid characters")
    private String idempotencyKey;

    @Size(max = 140, message = "Memo must not exceed 140 characters")
    private String memo;
}
