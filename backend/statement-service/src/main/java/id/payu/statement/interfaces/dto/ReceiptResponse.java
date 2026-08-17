package id.payu.statement.interfaces.dto;

import id.payu.statement.domain.model.ReceiptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for receipt response.
 * Epic E-19: Transaction Proof & Receipts (IMP-055)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptResponse {

    private UUID receiptId;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private String formattedAmount;

    // Sender info (masked)
    private String senderName;
    private String senderAccountMasked;
    private String senderBankName;

    // Recipient info (masked)
    private String recipientName;
    private String recipientAccountMasked;
    private String recipientBankName;

    private LocalDateTime timestamp;
    private String formattedTimestamp;
    private String referenceNumber;
    private ReceiptStatus status;
    private LocalDateTime expiryDate;
    private long daysUntilExpiry;
    private boolean isExpired;
}
