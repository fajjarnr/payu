package id.payu.transaction.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Request DTO for creating a disbursement (payout).
 */
public class CreateDisbursementRequest {
    public CreateDisbursementRequest() {
    }

    public CreateDisbursementRequest(BigDecimal amount, String bankCode, String accountNumber, String accountName, String description, String idempotencyKey) {
        this.amount = amount;
        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
    }

    public static CreateDisbursementRequestBuilder builder() {
        return new CreateDisbursementRequestBuilder();
    }

    public static class CreateDisbursementRequestBuilder {
        private BigDecimal amount;
        private String bankCode;
        private String accountNumber;
        private String accountName;
        private String description;
        private String idempotencyKey;

        public CreateDisbursementRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        public CreateDisbursementRequestBuilder bankCode(String bankCode) {
            this.bankCode = bankCode;
            return this;
        }
        public CreateDisbursementRequestBuilder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }
        public CreateDisbursementRequestBuilder accountName(String accountName) {
            this.accountName = accountName;
            return this;
        }
        public CreateDisbursementRequestBuilder description(String description) {
            this.description = description;
            return this;
        }
        public CreateDisbursementRequestBuilder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public CreateDisbursementRequest build() {
            return new CreateDisbursementRequest(amount, bankCode, accountNumber, accountName, description, idempotencyKey);
        }
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String currency = "IDR";

    @NotBlank(message = "Bank code is required")
    private String bankCode;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "Account name is required")
    private String accountName;

    private String description;

    private String idempotencyKey;
}
