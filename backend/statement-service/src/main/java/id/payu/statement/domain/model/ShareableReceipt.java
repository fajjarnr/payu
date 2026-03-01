package id.payu.statement.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Value Object representing a shareable version of a receipt.
 * Account numbers are masked for privacy/security.
 * Epic E-19: Transaction Proof & Receipts (IMP-055)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ShareableReceipt {

    private String receiptId;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime timestamp;
    private String referenceNumber;
    private ReceiptStatus status;

    // Sender info (masked)
    private String senderName;
    private String senderAccountMasked;
    private String senderBankName;

    // Recipient info (masked)
    private String recipientName;
    private String recipientAccountMasked;
    private String recipientBankName;
}
