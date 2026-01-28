package id.payu.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Transfer request DTO with comprehensive input validation.
 *
 * Security considerations:
 * - Account numbers validated to prevent injection
 * - Amount constraints prevent overflow attacks
 * - Description length prevents DoS via large payloads
 * - PIN validation for security-critical operations
 * - Device ID for fraud detection
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateTransferRequest {
    @NotNull(message = "Sender account ID is required")
    private UUID senderAccountId;

    @NotBlank(message = "Recipient account number is required")
    @Size(min = 10, max = 20, message = "Account number must be between 10 and 20 digits")
    @Pattern(regexp = "^[0-9]+$", message = "Account number must contain only digits")
    private String recipientAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Pattern(regexp = "^\\d{1,12}(\\.\\d{1,2})?$", message = "Amount must have maximum 12 digits and 2 decimal places")
    private BigDecimal amount;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code (e.g., IDR, USD)")
    private String currency;

    @NotBlank(message = "Description is required")
    @Size(min = 1, max = 100, message = "Description must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-.,]+$", message = "Description contains invalid characters")
    private String description;

    private TransactionType type;

    @Size(min = 6, max = 6, message = "Transaction PIN must be exactly 6 digits")
    @Pattern(regexp = "^\\d{6}$", message = "Transaction PIN must be exactly 6 digits")
    private String transactionPin;

    @Size(max = 100, message = "Device ID is too long")
    private String deviceId;

    @Size(max = 100, message = "Idempotency key is too long")
    @Pattern(regexp = "^[a-zA-Z0-9\\-]+$", message = "Idempotency key contains invalid characters")
    private String idempotencyKey;

    public enum TransactionType {
        INTERNAL_TRANSFER,
        BIFAST_TRANSFER,
        SKN_TRANSFER,
        RTGS_TRANSFER
    }
}
