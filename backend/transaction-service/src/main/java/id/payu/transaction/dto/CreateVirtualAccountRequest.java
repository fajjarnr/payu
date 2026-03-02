package id.payu.transaction.dto;

import id.payu.security.annotation.Sensitive;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request to create a Virtual Account for payment collection.
 */
public class CreateVirtualAccountRequest {
    public CreateVirtualAccountRequest() {
    }

    public CreateVirtualAccountRequest(String bankCode, UUID partnerId, BigDecimal amount, String currency, String description, String customerName, String customerEmail, String customerPhone, String externalId, String callbackUrl, Integer expiryHours) {
        this.bankCode = bankCode;
        this.partnerId = partnerId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.externalId = externalId;
        this.callbackUrl = callbackUrl;
        this.expiryHours = expiryHours;
    }

    public static CreateVirtualAccountRequestBuilder builder() {
        return new CreateVirtualAccountRequestBuilder();
    }

    public static class CreateVirtualAccountRequestBuilder {
        private String bankCode;
        private UUID partnerId;
        private BigDecimal amount;
        private String currency;
        private String description;
        private String customerName;
        private String customerEmail;
        private String customerPhone;
        private String externalId;
        private String callbackUrl;
        private Integer expiryHours;

        public CreateVirtualAccountRequestBuilder bankCode(String bankCode) {
            this.bankCode = bankCode;
            return this;
        }
        public CreateVirtualAccountRequestBuilder partnerId(UUID partnerId) {
            this.partnerId = partnerId;
            return this;
        }
        public CreateVirtualAccountRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        public CreateVirtualAccountRequestBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }
        public CreateVirtualAccountRequestBuilder description(String description) {
            this.description = description;
            return this;
        }
        public CreateVirtualAccountRequestBuilder customerName(String customerName) {
            this.customerName = customerName;
            return this;
        }
        public CreateVirtualAccountRequestBuilder customerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
            return this;
        }
        public CreateVirtualAccountRequestBuilder customerPhone(String customerPhone) {
            this.customerPhone = customerPhone;
            return this;
        }
        public CreateVirtualAccountRequestBuilder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }
        public CreateVirtualAccountRequestBuilder callbackUrl(String callbackUrl) {
            this.callbackUrl = callbackUrl;
            return this;
        }
        public CreateVirtualAccountRequestBuilder expiryHours(Integer expiryHours) {
            this.expiryHours = expiryHours;
            return this;
        }

        public CreateVirtualAccountRequest build() {
            return new CreateVirtualAccountRequest(bankCode, partnerId, amount, currency, description, customerName, customerEmail, customerPhone, externalId, callbackUrl, expiryHours);
        }
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public UUID getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(UUID partnerId) {
        this.partnerId = partnerId;
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

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public Integer getExpiryHours() {
        return expiryHours;
    }

    public void setExpiryHours(Integer expiryHours) {
        this.expiryHours = expiryHours;
    }



    @NotBlank(message = "Bank code is required")
    private String bankCode;

    @NotNull(message = "Partner ID is required")
    private UUID partnerId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1000", message = "Minimum amount is 1000")
    private BigDecimal amount;

    private String currency;

    private String description;

    @Sensitive
    private String customerName;

    @Sensitive
    private String customerEmail;

    @Sensitive
    private String customerPhone;

    private String externalId;

    private String callbackUrl;

    /** Expiry in hours (default: 24) */
    private Integer expiryHours;
}
