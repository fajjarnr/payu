package id.payu.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
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
}
