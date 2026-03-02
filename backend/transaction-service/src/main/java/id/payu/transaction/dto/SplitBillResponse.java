package id.payu.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class SplitBillResponse {

    private UUID id;
    private String referenceNumber;
    private UUID creatorAccountId;
    private BigDecimal totalAmount;
    private String currency;
    private String title;
    private String description;
    private String splitType;
    private String status;
    private Instant dueDate;
    private List<ParticipantResponse> participants;
    private BigDecimal totalPaid;
    private BigDecimal remainingAmount;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    public SplitBillResponse() {
    }

    public SplitBillResponse(UUID id, String referenceNumber, UUID creatorAccountId, BigDecimal totalAmount,
                             String currency, String title, String description, String splitType, String status,
                             Instant dueDate, List<ParticipantResponse> participants, BigDecimal totalPaid,
                             BigDecimal remainingAmount, Instant createdAt, Instant updatedAt, Instant completedAt) {
        this.id = id;
        this.referenceNumber = referenceNumber;
        this.creatorAccountId = creatorAccountId;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.title = title;
        this.description = description;
        this.splitType = splitType;
        this.status = status;
        this.dueDate = dueDate;
        this.participants = participants;
        this.totalPaid = totalPaid;
        this.remainingAmount = remainingAmount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
    }

    public static SplitBillResponseBuilder builder() {
        return new SplitBillResponseBuilder();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
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

    public String getSplitType() {
        return splitType;
    }

    public void setSplitType(String splitType) {
        this.splitType = splitType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public void setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
    }

    public List<ParticipantResponse> getParticipants() {
        return participants;
    }

    public void setParticipants(List<ParticipantResponse> participants) {
        this.participants = participants;
    }

    public BigDecimal getTotalPaid() {
        return totalPaid;
    }

    public void setTotalPaid(BigDecimal totalPaid) {
        this.totalPaid = totalPaid;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public static class SplitBillResponseBuilder {
        private UUID id;
        private String referenceNumber;
        private UUID creatorAccountId;
        private BigDecimal totalAmount;
        private String currency;
        private String title;
        private String description;
        private String splitType;
        private String status;
        private Instant dueDate;
        private List<ParticipantResponse> participants;
        private BigDecimal totalPaid;
        private BigDecimal remainingAmount;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant completedAt;

        public SplitBillResponseBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public SplitBillResponseBuilder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }

        public SplitBillResponseBuilder creatorAccountId(UUID creatorAccountId) {
            this.creatorAccountId = creatorAccountId;
            return this;
        }

        public SplitBillResponseBuilder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public SplitBillResponseBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public SplitBillResponseBuilder title(String title) {
            this.title = title;
            return this;
        }

        public SplitBillResponseBuilder description(String description) {
            this.description = description;
            return this;
        }

        public SplitBillResponseBuilder splitType(String splitType) {
            this.splitType = splitType;
            return this;
        }

        public SplitBillResponseBuilder status(String status) {
            this.status = status;
            return this;
        }

        public SplitBillResponseBuilder dueDate(Instant dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public SplitBillResponseBuilder participants(List<ParticipantResponse> participants) {
            this.participants = participants;
            return this;
        }

        public SplitBillResponseBuilder totalPaid(BigDecimal totalPaid) {
            this.totalPaid = totalPaid;
            return this;
        }

        public SplitBillResponseBuilder remainingAmount(BigDecimal remainingAmount) {
            this.remainingAmount = remainingAmount;
            return this;
        }

        public SplitBillResponseBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public SplitBillResponseBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public SplitBillResponseBuilder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public SplitBillResponse build() {
            return new SplitBillResponse(id, referenceNumber, creatorAccountId, totalAmount, currency, title,
                    description, splitType, status, dueDate, participants, totalPaid, remainingAmount,
                    createdAt, updatedAt, completedAt);
        }
    }

    public static class ParticipantResponse {
        private UUID id;
        private UUID accountId;
        private String accountNumber;
        private String accountName;
        private BigDecimal amountOwed;
        private BigDecimal amountPaid;
        private BigDecimal remainingAmount;
        private String status;
        private Instant settledAt;
        private Instant createdAt;
        private Instant updatedAt;

        public ParticipantResponse() {
        }

        public ParticipantResponse(UUID id, UUID accountId, String accountNumber, String accountName,
                                   BigDecimal amountOwed, BigDecimal amountPaid, BigDecimal remainingAmount,
                                   String status, Instant settledAt, Instant createdAt, Instant updatedAt) {
            this.id = id;
            this.accountId = accountId;
            this.accountNumber = accountNumber;
            this.accountName = accountName;
            this.amountOwed = amountOwed;
            this.amountPaid = amountPaid;
            this.remainingAmount = remainingAmount;
            this.status = status;
            this.settledAt = settledAt;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public static ParticipantResponseBuilder builder() {
            return new ParticipantResponseBuilder();
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
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

        public BigDecimal getAmountPaid() {
            return amountPaid;
        }

        public void setAmountPaid(BigDecimal amountPaid) {
            this.amountPaid = amountPaid;
        }

        public BigDecimal getRemainingAmount() {
            return remainingAmount;
        }

        public void setRemainingAmount(BigDecimal remainingAmount) {
            this.remainingAmount = remainingAmount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Instant getSettledAt() {
            return settledAt;
        }

        public void setSettledAt(Instant settledAt) {
            this.settledAt = settledAt;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }

        public static class ParticipantResponseBuilder {
            private UUID id;
            private UUID accountId;
            private String accountNumber;
            private String accountName;
            private BigDecimal amountOwed;
            private BigDecimal amountPaid;
            private BigDecimal remainingAmount;
            private String status;
            private Instant settledAt;
            private Instant createdAt;
            private Instant updatedAt;

            public ParticipantResponseBuilder id(UUID id) {
                this.id = id;
                return this;
            }

            public ParticipantResponseBuilder accountId(UUID accountId) {
                this.accountId = accountId;
                return this;
            }

            public ParticipantResponseBuilder accountNumber(String accountNumber) {
                this.accountNumber = accountNumber;
                return this;
            }

            public ParticipantResponseBuilder accountName(String accountName) {
                this.accountName = accountName;
                return this;
            }

            public ParticipantResponseBuilder amountOwed(BigDecimal amountOwed) {
                this.amountOwed = amountOwed;
                return this;
            }

            public ParticipantResponseBuilder amountPaid(BigDecimal amountPaid) {
                this.amountPaid = amountPaid;
                return this;
            }

            public ParticipantResponseBuilder remainingAmount(BigDecimal remainingAmount) {
                this.remainingAmount = remainingAmount;
                return this;
            }

            public ParticipantResponseBuilder status(String status) {
                this.status = status;
                return this;
            }

            public ParticipantResponseBuilder settledAt(Instant settledAt) {
                this.settledAt = settledAt;
                return this;
            }

            public ParticipantResponseBuilder createdAt(Instant createdAt) {
                this.createdAt = createdAt;
                return this;
            }

            public ParticipantResponseBuilder updatedAt(Instant updatedAt) {
                this.updatedAt = updatedAt;
                return this;
            }

            public ParticipantResponse build() {
                return new ParticipantResponse(id, accountId, accountNumber, accountName, amountOwed, amountPaid,
                        remainingAmount, status, settledAt, createdAt, updatedAt);
            }
        }
    }
}
