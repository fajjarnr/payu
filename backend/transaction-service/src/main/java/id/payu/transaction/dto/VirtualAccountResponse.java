package id.payu.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for Virtual Account details.
 */
public class VirtualAccountResponse {
    public VirtualAccountResponse() {
    }

    public VirtualAccountResponse(UUID id, String vaNumber, String bankCode, String bankName, UUID partnerId, String externalId, BigDecimal amount, String currency, String description, String customerName, String status, BigDecimal paidAmount, Instant paidAt, String paymentReference, Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.vaNumber = vaNumber;
        this.bankCode = bankCode;
        this.bankName = bankName;
        this.partnerId = partnerId;
        this.externalId = externalId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.customerName = customerName;
        this.status = status;
        this.paidAmount = paidAmount;
        this.paidAt = paidAt;
        this.paymentReference = paymentReference;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static VirtualAccountResponseBuilder builder() {
        return new VirtualAccountResponseBuilder();
    }

    public static class VirtualAccountResponseBuilder {
        private UUID id;
        private String vaNumber;
        private String bankCode;
        private String bankName;
        private UUID partnerId;
        private String externalId;
        private BigDecimal amount;
        private String currency;
        private String description;
        private String customerName;
        private String status;
        private BigDecimal paidAmount;
        private Instant paidAt;
        private String paymentReference;
        private Instant expiresAt;
        private Instant createdAt;

        public VirtualAccountResponseBuilder id(UUID id) {
            this.id = id;
            return this;
        }
        public VirtualAccountResponseBuilder vaNumber(String vaNumber) {
            this.vaNumber = vaNumber;
            return this;
        }
        public VirtualAccountResponseBuilder bankCode(String bankCode) {
            this.bankCode = bankCode;
            return this;
        }
        public VirtualAccountResponseBuilder bankName(String bankName) {
            this.bankName = bankName;
            return this;
        }
        public VirtualAccountResponseBuilder partnerId(UUID partnerId) {
            this.partnerId = partnerId;
            return this;
        }
        public VirtualAccountResponseBuilder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }
        public VirtualAccountResponseBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        public VirtualAccountResponseBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }
        public VirtualAccountResponseBuilder description(String description) {
            this.description = description;
            return this;
        }
        public VirtualAccountResponseBuilder customerName(String customerName) {
            this.customerName = customerName;
            return this;
        }
        public VirtualAccountResponseBuilder status(String status) {
            this.status = status;
            return this;
        }
        public VirtualAccountResponseBuilder paidAmount(BigDecimal paidAmount) {
            this.paidAmount = paidAmount;
            return this;
        }
        public VirtualAccountResponseBuilder paidAt(Instant paidAt) {
            this.paidAt = paidAt;
            return this;
        }
        public VirtualAccountResponseBuilder paymentReference(String paymentReference) {
            this.paymentReference = paymentReference;
            return this;
        }
        public VirtualAccountResponseBuilder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        public VirtualAccountResponseBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public VirtualAccountResponse build() {
            return new VirtualAccountResponse(id, vaNumber, bankCode, bankName, partnerId, externalId, amount, currency, description, customerName, status, paidAmount, paidAt, paymentReference, expiresAt, createdAt);
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getVaNumber() {
        return vaNumber;
    }

    public void setVaNumber(String vaNumber) {
        this.vaNumber = vaNumber;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public UUID getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(UUID partnerId) {
        this.partnerId = partnerId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
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

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }



    private UUID id;
    private String vaNumber;
    private String bankCode;
    private String bankName;
    private UUID partnerId;
    private String externalId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String customerName;
    private String status;
    private BigDecimal paidAmount;
    private Instant paidAt;
    private String paymentReference;
    private Instant expiresAt;
    private Instant createdAt;
}
