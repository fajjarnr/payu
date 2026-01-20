package id.payu.wallet.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ReserveBalanceRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Reference ID is required")
    private String referenceId;

    public ReserveBalanceRequest() {
    }

    public ReserveBalanceRequest(BigDecimal amount, String referenceId) {
        this.amount = amount;
        this.referenceId = referenceId;
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
}
