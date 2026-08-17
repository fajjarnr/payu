package id.payu.transaction.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class BifastTransferRequest {
    @NotBlank(message = "Reference number is required")
    private String referenceNumber;

    @NotBlank(message = "Beneficiary bank code is required")
    private String beneficiaryBankCode;

    @NotBlank(message = "Beneficiary account number is required")
    private String beneficiaryAccountNumber;

    @NotBlank(message = "Beneficiary account name is required")
    private String beneficiaryAccountName;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private String currency;

    @NotBlank(message = "Sender account number is required")
    private String senderAccountNumber;

    @NotBlank(message = "Sender account name is required")
    private String senderAccountName;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Purpose code must be 3 uppercase letters")
    private String purposeCode;

    @Size(max = 500)
    private String webhookUrl;

    public BifastTransferRequest() {
    }

    public BifastTransferRequest(String referenceNumber, String beneficiaryBankCode, String beneficiaryAccountNumber,
                                 String beneficiaryAccountName, BigDecimal amount, String currency,
                                 String senderAccountNumber, String senderAccountName, String purposeCode) {
        this.referenceNumber = referenceNumber;
        this.beneficiaryBankCode = beneficiaryBankCode;
        this.beneficiaryAccountNumber = beneficiaryAccountNumber;
        this.beneficiaryAccountName = beneficiaryAccountName;
        this.amount = amount;
        this.currency = currency;
        this.senderAccountNumber = senderAccountNumber;
        this.senderAccountName = senderAccountName;
        this.purposeCode = purposeCode;
    }

    public static BifastTransferRequestBuilder builder() {
        return new BifastTransferRequestBuilder();
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getBeneficiaryBankCode() {
        return beneficiaryBankCode;
    }

    public void setBeneficiaryBankCode(String beneficiaryBankCode) {
        this.beneficiaryBankCode = beneficiaryBankCode;
    }

    public String getBeneficiaryAccountNumber() {
        return beneficiaryAccountNumber;
    }

    public void setBeneficiaryAccountNumber(String beneficiaryAccountNumber) {
        this.beneficiaryAccountNumber = beneficiaryAccountNumber;
    }

    public String getBeneficiaryAccountName() {
        return beneficiaryAccountName;
    }

    public void setBeneficiaryAccountName(String beneficiaryAccountName) {
        this.beneficiaryAccountName = beneficiaryAccountName;
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

    public String getSenderAccountNumber() {
        return senderAccountNumber;
    }

    public void setSenderAccountNumber(String senderAccountNumber) {
        this.senderAccountNumber = senderAccountNumber;
    }

    public String getSenderAccountName() {
        return senderAccountName;
    }

    public void setSenderAccountName(String senderAccountName) {
        this.senderAccountName = senderAccountName;
    }

    public String getPurposeCode() {
        return purposeCode;
    }

    public void setPurposeCode(String purposeCode) {
        this.purposeCode = purposeCode;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public static class BifastTransferRequestBuilder {
        private String referenceNumber;
        private String beneficiaryBankCode;
        private String beneficiaryAccountNumber;
        private String beneficiaryAccountName;
        private BigDecimal amount;
        private String currency;
        private String senderAccountNumber;
        private String senderAccountName;
        private String purposeCode;
        private String webhookUrl;

        public BifastTransferRequestBuilder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }

        public BifastTransferRequestBuilder beneficiaryBankCode(String beneficiaryBankCode) {
            this.beneficiaryBankCode = beneficiaryBankCode;
            return this;
        }

        public BifastTransferRequestBuilder beneficiaryAccountNumber(String beneficiaryAccountNumber) {
            this.beneficiaryAccountNumber = beneficiaryAccountNumber;
            return this;
        }

        public BifastTransferRequestBuilder beneficiaryAccountName(String beneficiaryAccountName) {
            this.beneficiaryAccountName = beneficiaryAccountName;
            return this;
        }

        public BifastTransferRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public BifastTransferRequestBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public BifastTransferRequestBuilder senderAccountNumber(String senderAccountNumber) {
            this.senderAccountNumber = senderAccountNumber;
            return this;
        }

        public BifastTransferRequestBuilder senderAccountName(String senderAccountName) {
            this.senderAccountName = senderAccountName;
            return this;
        }

        public BifastTransferRequestBuilder purposeCode(String purposeCode) {
            this.purposeCode = purposeCode;
            return this;
        }

        public BifastTransferRequestBuilder webhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
            return this;
        }

        public BifastTransferRequest build() {
            BifastTransferRequest request = new BifastTransferRequest(referenceNumber, beneficiaryBankCode, beneficiaryAccountNumber,
                    beneficiaryAccountName, amount, currency, senderAccountNumber, senderAccountName, purposeCode);
            request.setWebhookUrl(webhookUrl);
            return request;
        }
    }
}
