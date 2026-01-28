package id.payu.transaction.application.cqrs.command;

import id.payu.transaction.application.cqrs.Command;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.domain.model.Money;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command to initiate a fund transfer.
 * This is a write operation that modifies system state.
 */
public record InitiateTransferCommand(
        UUID senderAccountId,
        String recipientAccountNumber,
        Money amount,
        String description,
        InitiateTransferRequest.TransactionType type,
        String transactionPin,
        String deviceId,
        String idempotencyKey,
        String userId
) implements Command<InitiateTransferCommandResult> {

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
