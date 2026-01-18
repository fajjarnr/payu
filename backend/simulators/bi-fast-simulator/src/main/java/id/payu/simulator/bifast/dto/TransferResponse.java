package id.payu.simulator.bifast.dto;

import id.payu.simulator.bifast.entity.Transfer;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for fund transfer.
 */
public record TransferResponse(
    String referenceNumber,
    String sourceBankCode,
    String sourceAccountNumber,
    String destinationBankCode,
    String destinationAccountNumber,
    String destinationAccountName,
    BigDecimal amount,
    String currency,
    String status,
    String responseCode,
    String responseMessage,
    LocalDateTime createdAt,
    LocalDateTime completedAt
) {
    public static TransferResponse fromEntity(Transfer transfer) {
        String responseCode = switch (transfer.status) {
            case COMPLETED -> "00";
            case PENDING, PROCESSING -> "09";
            case FAILED -> "51";
            case TIMEOUT -> "68";
        };
        
        String responseMessage = switch (transfer.status) {
            case COMPLETED -> "Transfer completed successfully";
            case PENDING -> "Transfer pending";
            case PROCESSING -> "Transfer in progress";
            case FAILED -> transfer.failureReason != null ? transfer.failureReason : "Transfer failed";
            case TIMEOUT -> "Transfer timeout";
        };

        return new TransferResponse(
            transfer.referenceNumber,
            transfer.sourceBankCode,
            transfer.sourceAccountNumber,
            transfer.destinationBankCode,
            transfer.destinationAccountNumber,
            transfer.destinationAccountName,
            transfer.amount,
            transfer.currency,
            transfer.status.name(),
            responseCode,
            responseMessage,
            transfer.createdAt,
            transfer.completedAt
        );
    }

    public static TransferResponse error(String message) {
        return new TransferResponse(
            null, null, null, null, null, null,
            null, null, "ERROR", "96", message,
            null, null
        );
    }
}
