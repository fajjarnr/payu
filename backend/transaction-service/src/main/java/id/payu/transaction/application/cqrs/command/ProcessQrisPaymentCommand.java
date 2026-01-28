package id.payu.transaction.application.cqrs.command;

import id.payu.transaction.application.cqrs.Command;
import id.payu.transaction.dto.ProcessQrisPaymentRequest;
import id.payu.transaction.domain.model.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Command to process a QRIS payment.
 * This is a write operation that modifies system state.
 *
 * <p>Validation Rules:</p>
 * <ul>
 *   <li>qrisCode: Required, valid QRIS code format</li>
 *   <li>amount: Required, positive amount (validated by Money)</li>
 *   <li>userId: Required</li>
 * </ul>
 */
public record ProcessQrisPaymentCommand(
        @NotBlank(message = "QRIS code is required")
        @Size(min = 8, max = 50, message = "QRIS code must be between 8 and 50 characters")
        @Pattern(regexp = "^[0-9]+$", message = "QRIS code must contain only digits")
        String qrisCode,

        @NotNull(message = "Payment amount is required")
        Money amount,

        @NotBlank(message = "User ID is required")
        String userId
) implements Command<Void> {

    /**
     * Compact constructor for additional validation.
     */
    public ProcessQrisPaymentCommand {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(userId, "User ID cannot be null");

        // Additional validation for Money positivity
        if (amount.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount.getAmount());
        }
    }

    /**
     * Factory method to create command from DTO.
     */
    public static ProcessQrisPaymentCommand from(ProcessQrisPaymentRequest request, String userId) {
        Money money = request.getCurrency() != null
                ? Money.of(request.getAmount(), request.getCurrency())
                : Money.idr(request.getAmount());

        return new ProcessQrisPaymentCommand(
                request.getQrisCode(),
                money,
                userId
        );
    }
}
