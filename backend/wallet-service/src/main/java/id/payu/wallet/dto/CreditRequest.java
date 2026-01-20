package id.payu.wallet.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CreditRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Reference ID is required")
    private String referenceId;

    private String description;

    public CreditRequest() {
    }

    public CreditRequest(BigDecimal amount, String referenceId, String description) {
        this.amount = amount;
        this.referenceId = referenceId;
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
