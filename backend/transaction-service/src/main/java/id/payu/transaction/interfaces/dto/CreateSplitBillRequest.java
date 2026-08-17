package id.payu.transaction.interfaces.dto;

import id.payu.transaction.adapter.persistence.entity.SplitBillEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import id.payu.transaction.domain.model.SplitType;

public class CreateSplitBillRequest {

    @NotNull(message = "Creator account ID is required")
    private UUID creatorAccountId;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal totalAmount;

    private String currency;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Split type is required")
    private SplitType splitType;

    private Instant dueDate;

    @NotEmpty(message = "At least one participant is required")
    private List<ParticipantRequest> participants;

    public CreateSplitBillRequest() {
    }

    public CreateSplitBillRequest(UUID creatorAccountId, BigDecimal totalAmount, String currency, String title,
                                  String description, SplitType splitType, Instant dueDate,
                                  List<ParticipantRequest> participants) {
        this.creatorAccountId = creatorAccountId;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.title = title;
        this.description = description;
        this.splitType = splitType;
        this.dueDate = dueDate;
        this.participants = participants;
    }

    public static CreateSplitBillRequestBuilder builder() {
        return new CreateSplitBillRequestBuilder();
    }

    public UUID getCreatorAccountId() {
        return creatorAccountId;
    }

    public void setCreatorAccountId(UUID creatorAccountId) {
        this.creatorAccountId = creatorAccountId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public void setSplitType(SplitType splitType) {
        this.splitType = splitType;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public void setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
    }

    public List<ParticipantRequest> getParticipants() {
        return participants;
    }

    public void setParticipants(List<ParticipantRequest> participants) {
        this.participants = participants;
    }

    public static class CreateSplitBillRequestBuilder {
        private UUID creatorAccountId;
        private BigDecimal totalAmount;
        private String currency;
        private String title;
        private String description;
        private SplitType splitType;
        private Instant dueDate;
        private List<ParticipantRequest> participants;

        public CreateSplitBillRequestBuilder creatorAccountId(UUID creatorAccountId) {
            this.creatorAccountId = creatorAccountId;
            return this;
        }

        public CreateSplitBillRequestBuilder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public CreateSplitBillRequestBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public CreateSplitBillRequestBuilder title(String title) {
            this.title = title;
            return this;
        }

        public CreateSplitBillRequestBuilder description(String description) {
            this.description = description;
            return this;
        }

        public CreateSplitBillRequestBuilder splitType(SplitType splitType) {
            this.splitType = splitType;
            return this;
        }

        public CreateSplitBillRequestBuilder dueDate(Instant dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public CreateSplitBillRequestBuilder participants(List<ParticipantRequest> participants) {
            this.participants = participants;
            return this;
        }

        public CreateSplitBillRequest build() {
            return new CreateSplitBillRequest(creatorAccountId, totalAmount, currency, title, description,
                    splitType, dueDate, participants);
        }
    }

    public static class ParticipantRequest {
        @NotNull(message = "Account ID is required")
        private UUID accountId;

        @NotBlank(message = "Account number is required")
        private String accountNumber;

        @NotBlank(message = "Account name is required")
        private String accountName;

        private BigDecimal amountOwed;

        private BigDecimal percentage;

        private Boolean isCreator;

        public ParticipantRequest() {
        }

        public ParticipantRequest(UUID accountId, String accountNumber, String accountName,
                                  BigDecimal amountOwed, BigDecimal percentage, Boolean isCreator) {
            this.accountId = accountId;
            this.accountNumber = accountNumber;
            this.accountName = accountName;
            this.amountOwed = amountOwed;
            this.percentage = percentage;
            this.isCreator = isCreator;
        }

        public static ParticipantRequestBuilder builder() {
            return new ParticipantRequestBuilder();
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

        public Boolean getIsCreator() {
            return isCreator;
        }

        public void setIsCreator(Boolean isCreator) {
            this.isCreator = isCreator;
        }

        public static class ParticipantRequestBuilder {
            private UUID accountId;
            private String accountNumber;
            private String accountName;
            private BigDecimal amountOwed;
            private BigDecimal percentage;
            private Boolean isCreator;

            public ParticipantRequestBuilder accountId(UUID accountId) {
                this.accountId = accountId;
                return this;
            }

            public ParticipantRequestBuilder accountNumber(String accountNumber) {
                this.accountNumber = accountNumber;
                return this;
            }

            public ParticipantRequestBuilder accountName(String accountName) {
                this.accountName = accountName;
                return this;
            }

            public ParticipantRequestBuilder amountOwed(BigDecimal amountOwed) {
                this.amountOwed = amountOwed;
                return this;
            }

            public ParticipantRequestBuilder percentage(BigDecimal percentage) {
                this.percentage = percentage;
                return this;
            }

            public ParticipantRequestBuilder isCreator(Boolean isCreator) {
                this.isCreator = isCreator;
                return this;
            }

            public ParticipantRequest build() {
                return new ParticipantRequest(accountId, accountNumber, accountName, amountOwed, percentage, isCreator);
            }
        }
    }
}
