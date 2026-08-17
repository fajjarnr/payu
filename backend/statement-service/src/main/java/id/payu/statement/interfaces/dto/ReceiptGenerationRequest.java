package id.payu.statement.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for receipt generation request.
 * Epic E-19: Transaction Proof & Receipts (IMP-055)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptGenerationRequest {

    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    private String customerId;
}
