package id.payu.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for refunding an escrow transaction.
 */
public class RefundEscrowRequest {

    @NotBlank(message = "Refund reason is required")
    @Size(max = 512, message = "Reason must be 512 characters or less")
    private String reason;

    public RefundEscrowRequest() {
    }

    public RefundEscrowRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
