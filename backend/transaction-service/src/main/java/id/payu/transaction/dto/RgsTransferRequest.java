package id.payu.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public class RgsTransferRequest {
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

    @Pattern(regexp = "^[A-Z0-9]{3}$", message = "Sender type code must be 3 alphanumeric characters")
    private String senderTypeCode;

    private String senderResidentCode;

    private String senderIdNumber;

    public RgsTransferRequest() {
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

    public String getSenderTypeCode() {
        return senderTypeCode;
    }

    public void setSenderTypeCode(String senderTypeCode) {
        this.senderTypeCode = senderTypeCode;
    }

    public String getSenderResidentCode() {
        return senderResidentCode;
    }

    public void setSenderResidentCode(String senderResidentCode) {
        this.senderResidentCode = senderResidentCode;
    }

    public String getSenderIdNumber() {
        return senderIdNumber;
    }

    public void setSenderIdNumber(String senderIdNumber) {
        this.senderIdNumber = senderIdNumber;
    }
}
