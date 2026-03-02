package id.payu.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public class SknTransferRequest {
    public SknTransferRequest() {
    }

    public SknTransferRequest(String referenceNumber, String beneficiaryBankCode, String beneficiaryAccountNumber, String beneficiaryAccountName, BigDecimal amount, String currency, String senderAccountNumber, String senderAccountName, String beneficiaryBankName, String purposeCode, String beneficiaryTypeCode, String beneficiaryResidentCode, String beneficiaryIdNumber) {
        this.referenceNumber = referenceNumber;
        this.beneficiaryBankCode = beneficiaryBankCode;
        this.beneficiaryAccountNumber = beneficiaryAccountNumber;
        this.beneficiaryAccountName = beneficiaryAccountName;
        this.amount = amount;
        this.currency = currency;
        this.senderAccountNumber = senderAccountNumber;
        this.senderAccountName = senderAccountName;
        this.beneficiaryBankName = beneficiaryBankName;
        this.purposeCode = purposeCode;
        this.beneficiaryTypeCode = beneficiaryTypeCode;
        this.beneficiaryResidentCode = beneficiaryResidentCode;
        this.beneficiaryIdNumber = beneficiaryIdNumber;
    }

    public static SknTransferRequestBuilder builder() {
        return new SknTransferRequestBuilder();
    }

    public static class SknTransferRequestBuilder {
        private String referenceNumber;
        private String beneficiaryBankCode;
        private String beneficiaryAccountNumber;
        private String beneficiaryAccountName;
        private BigDecimal amount;
        private String currency;
        private String senderAccountNumber;
        private String senderAccountName;
        private String beneficiaryBankName;
        private String purposeCode;
        private String beneficiaryTypeCode;
        private String beneficiaryResidentCode;
        private String beneficiaryIdNumber;

        public SknTransferRequestBuilder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }
        public SknTransferRequestBuilder beneficiaryBankCode(String beneficiaryBankCode) {
            this.beneficiaryBankCode = beneficiaryBankCode;
            return this;
        }
        public SknTransferRequestBuilder beneficiaryAccountNumber(String beneficiaryAccountNumber) {
            this.beneficiaryAccountNumber = beneficiaryAccountNumber;
            return this;
        }
        public SknTransferRequestBuilder beneficiaryAccountName(String beneficiaryAccountName) {
            this.beneficiaryAccountName = beneficiaryAccountName;
            return this;
        }
        public SknTransferRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        public SknTransferRequestBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }
        public SknTransferRequestBuilder senderAccountNumber(String senderAccountNumber) {
            this.senderAccountNumber = senderAccountNumber;
            return this;
        }
        public SknTransferRequestBuilder senderAccountName(String senderAccountName) {
            this.senderAccountName = senderAccountName;
            return this;
        }
        public SknTransferRequestBuilder beneficiaryBankName(String beneficiaryBankName) {
            this.beneficiaryBankName = beneficiaryBankName;
            return this;
        }
        public SknTransferRequestBuilder purposeCode(String purposeCode) {
            this.purposeCode = purposeCode;
            return this;
        }
        public SknTransferRequestBuilder beneficiaryTypeCode(String beneficiaryTypeCode) {
            this.beneficiaryTypeCode = beneficiaryTypeCode;
            return this;
        }
        public SknTransferRequestBuilder beneficiaryResidentCode(String beneficiaryResidentCode) {
            this.beneficiaryResidentCode = beneficiaryResidentCode;
            return this;
        }
        public SknTransferRequestBuilder beneficiaryIdNumber(String beneficiaryIdNumber) {
            this.beneficiaryIdNumber = beneficiaryIdNumber;
            return this;
        }

        public SknTransferRequest build() {
            return new SknTransferRequest(referenceNumber, beneficiaryBankCode, beneficiaryAccountNumber, beneficiaryAccountName, amount, currency, senderAccountNumber, senderAccountName, beneficiaryBankName, purposeCode, beneficiaryTypeCode, beneficiaryResidentCode, beneficiaryIdNumber);
        }
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

    public String getBeneficiaryBankName() {
        return beneficiaryBankName;
    }

    public void setBeneficiaryBankName(String beneficiaryBankName) {
        this.beneficiaryBankName = beneficiaryBankName;
    }

    public String getPurposeCode() {
        return purposeCode;
    }

    public void setPurposeCode(String purposeCode) {
        this.purposeCode = purposeCode;
    }

    public String getBeneficiaryTypeCode() {
        return beneficiaryTypeCode;
    }

    public void setBeneficiaryTypeCode(String beneficiaryTypeCode) {
        this.beneficiaryTypeCode = beneficiaryTypeCode;
    }

    public String getBeneficiaryResidentCode() {
        return beneficiaryResidentCode;
    }

    public void setBeneficiaryResidentCode(String beneficiaryResidentCode) {
        this.beneficiaryResidentCode = beneficiaryResidentCode;
    }

    public String getBeneficiaryIdNumber() {
        return beneficiaryIdNumber;
    }

    public void setBeneficiaryIdNumber(String beneficiaryIdNumber) {
        this.beneficiaryIdNumber = beneficiaryIdNumber;
    }


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

    @NotBlank(message = "Beneficiary bank name is required")
    private String beneficiaryBankName;

    @Pattern(regexp = "^[A-Z0-9]{4}$", message = "Purpose code must be 4 alphanumeric characters")
    private String purposeCode;

    @Pattern(regexp = "^[A-Z0-9]{3}$", message = "Beneficiary type code must be 3 alphanumeric characters")
    private String beneficiaryTypeCode;

    private String beneficiaryResidentCode;

    private String beneficiaryIdNumber;
}
