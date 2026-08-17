package id.payu.transaction.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.UUID;

public class AddParticipantRequest {
    public AddParticipantRequest() {
    }

    public AddParticipantRequest(UUID accountId, String accountNumber, String accountName, BigDecimal amountOwed, BigDecimal percentage) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.amountOwed = amountOwed;
        this.percentage = percentage;
    }

    public static AddParticipantRequestBuilder builder() {
        return new AddParticipantRequestBuilder();
    }

    public static class AddParticipantRequestBuilder {
        private UUID accountId;
        private String accountNumber;
        private String accountName;
        private BigDecimal amountOwed;
        private BigDecimal percentage;

        public AddParticipantRequestBuilder accountId(UUID accountId) {
            this.accountId = accountId;
            return this;
        }
        public AddParticipantRequestBuilder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }
        public AddParticipantRequestBuilder accountName(String accountName) {
            this.accountName = accountName;
            return this;
        }
        public AddParticipantRequestBuilder amountOwed(BigDecimal amountOwed) {
            this.amountOwed = amountOwed;
            return this;
        }
        public AddParticipantRequestBuilder percentage(BigDecimal percentage) {
            this.percentage = percentage;
            return this;
        }

        public AddParticipantRequest build() {
            return new AddParticipantRequest(accountId, accountNumber, accountName, amountOwed, percentage);
        }
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
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

    public BigDecimal getAmountOwed() {
        return amountOwed;
    }

    public void setAmountOwed(BigDecimal amountOwed) {
        this.amountOwed = amountOwed;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }


    @NotNull(message = "Account ID is required")
    private UUID accountId;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "Account name is required")
    private String accountName;

    private BigDecimal amountOwed;

    private BigDecimal percentage;
}
