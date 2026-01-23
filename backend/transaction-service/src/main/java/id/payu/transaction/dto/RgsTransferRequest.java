package id.payu.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
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
}
