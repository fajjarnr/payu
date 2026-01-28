package id.payu.transaction.application.cqrs.command;

import id.payu.transaction.application.cqrs.Command;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.domain.model.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.Objects;

/**
 * Command to initiate a fund transfer.
 * This is a write operation that modifies system state.
 *
 * <p>Validation Rules:</p>
 * <ul>
 *   <li>senderAccountId: Required, must be valid UUID</li>
 *   <li>recipientAccountNumber: Required, 10-20 digits</li>
 *   <li>amount: Required, positive amount (validated by Money)</li>
 *   <li>description: Required, max 100 characters</li>
 *   <li>type: Required</li>
 *   <li>userId: Required</li>
 * </ul>
 */
public record InitiateTransferCommand(
        @NotNull(message = "Sender account ID is required")
        UUID senderAccountId,

        @NotBlank(message = "Recipient account number is required")
        @Size(min = 10, max = 20, message = "Account number must be between 10 and 20 digits")
        @Pattern(regexp = "^[0-9]+$", message = "Account number must contain only digits")
        String recipientAccountNumber,

        @NotNull(message = "Transfer amount is required")
        Money amount,

        @NotBlank(message = "Description is required")
        @Size(min = 1, max = 100, message = "Description must be between 1 and 100 characters")
        @Pattern(regexp = "^[a-zA-Z0-9\\s\\-.,]+$", message = "Description contains invalid characters")
        String description,

        @NotNull(message = "Transaction type is required")
        InitiateTransferRequest.TransactionType type,

        @Pattern(regexp = "^\\d{6}$", message = "Transaction PIN must be exactly 6 digits")
        String transactionPin,

        @Size(max = 100, message = "Device ID is too long")
        String deviceId,

        @Size(max = 100, message = "Idempotency key is too long")
        @Pattern(regexp = "^[a-zA-Z0-9\\-]+$", message = "Idempotency key contains invalid characters")
        String idempotencyKey,

        @NotBlank(message = "User ID is required")
        String userId
) implements Command<InitiateTransferCommandResult> {

    /**
     * Compact constructor for additional validation.
     */
    public InitiateTransferCommand {
        Objects.requireNonNull(senderAccountId, "Sender account ID cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(type, "Transaction type cannot be null");
        Objects.requireNonNull(userId, "User ID cannot be null");

        // Additional validation for Money positivity
        if (amount.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount.getAmount());
        }
    }

    /**
     * Factory method to create command from DTO.
     */
    public static InitiateTransferCommand from(InitiateTransferRequest request, String userId) {
        // Convert BigDecimal amount to Money Value Object
        Money money = request.getCurrency() != null
                ? Money.of(request.getAmount(), request.getCurrency())
                : Money.idr(request.getAmount());

        return new InitiateTransferCommand(
                request.getSenderAccountId(),
                request.getRecipientAccountNumber(),
                money,
                request.getDescription(),
                request.getType(),
                request.getTransactionPin(),
                request.getDeviceId(),
                request.getIdempotencyKey(),
                userId
        );
    }
}
