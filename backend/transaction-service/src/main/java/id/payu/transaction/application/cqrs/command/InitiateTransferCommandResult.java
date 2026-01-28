package id.payu.transaction.application.cqrs.command;

import id.payu.transaction.dto.InitiateTransferResponse;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Result of initiating a transfer.
 * Contains only essential data (ID, status), not full entity state.
 */
public record InitiateTransferCommandResult(
        UUID transactionId,
        String referenceNumber,
        String status,
        BigDecimal fee,
        String estimatedCompletionTime
) {
    /**
     * Converts to DTO for API response.
     */
    public InitiateTransferResponse toResponse() {
        return new InitiateTransferResponse(
                transactionId,
                referenceNumber,
                status,
                fee,
                estimatedCompletionTime
        );
    }
}
