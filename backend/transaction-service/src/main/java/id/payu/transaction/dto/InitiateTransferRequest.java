package id.payu.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateTransferRequest {
    @NotNull(message = "Sender account ID is required")
    private UUID senderAccountId;

    @NotBlank(message = "Recipient account number is required")
    private String recipientAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String currency;

    @NotBlank(message = "Description is required")
    private String description;

    private TransactionType type;

    private String transactionPin;
    private String deviceId;

    public enum TransactionType {
        INTERNAL_TRANSFER,
        BIFAST_TRANSFER,
        SKN_TRANSFER,
        RTGS_TRANSFER
    }

    private String idempotencyKey;
}
