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
        Money money = request.currency() != null
                ? Money.of(request.amount(), request.currency())
                : Money.idr(request.amount());

        return new InitiateTransferCommand(
                request.senderAccountId(),
                request.recipientAccountNumber(),
                money,
                request.description(),
                request.type(),
                request.transactionPin(),
                request.deviceId(),
                request.idempotencyKey(),
                userId
        );
    }
}
