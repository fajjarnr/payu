package id.payu.statement.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * Aggregate Root representing a transaction receipt (bukti transfer).
 * This is a separate concept from monthly e-statements - it's a per-transaction
 * proof that users can download and share.
 * <p>
 * Epic E-19: Transaction Proof & Receipts (IMP-055)
 * <p>
 * Business Rules:
 * - Receipts are valid for 90 days from generation
 * - Account numbers are masked in shareable format
 * - Each receipt has a unique ID separate from transaction ID
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Receipt {

    private static final int EXPIRY_DAYS = 90;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm 'WIB'", new Locale("id", "ID"));

    private UUID id;
    private String transactionId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private SenderInfo senderInfo;
    private RecipientInfo recipientInfo;
    private LocalDateTime timestamp;
    private ReceiptStatus status;
    private String referenceNumber;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
    private int accessCount;

    /**
     * Factory method to generate a new receipt.
     *
     * @param transactionId   The transaction ID
     * @param amount          The transaction amount (must be positive)
     * @param currency        The currency code (e.g., "IDR")
     * @param senderInfo      Sender information (value object)
     * @param recipientInfo   Recipient information (value object)
     * @param referenceNumber Bank reference number
     * @return A new Receipt instance with GENERATED status
     * @throws IllegalArgumentException if any validation fails
     */
    public static Receipt generate(String transactionId,
                                    String customerId,
                                    BigDecimal amount,
                                    String currency,
                                    SenderInfo senderInfo,
                                    RecipientInfo recipientInfo,
                                    String referenceNumber) {
        validateInputs(transactionId, customerId, amount, senderInfo, recipientInfo, referenceNumber);

        LocalDateTime now = LocalDateTime.now();

        return Receipt.builder()
                .id(UUID.randomUUID())
                .transactionId(transactionId)
                .customerId(customerId)
                .amount(amount)
                .currency(currency != null ? currency : "IDR")
                .senderInfo(senderInfo)
                .recipientInfo(recipientInfo)
                .timestamp(now)
                .status(ReceiptStatus.GENERATED)
                .referenceNumber(referenceNumber)
                .expiryDate(now.plusDays(EXPIRY_DAYS))
                .createdAt(now)
                .accessCount(0)
                .build();
    }

    /**
     * Validates all inputs for receipt generation.
     */
    private static void validateInputs(String transactionId,
                                        String customerId,
                                        BigDecimal amount,
                                        SenderInfo senderInfo,
                                        RecipientInfo recipientInfo,
                                        String referenceNumber) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (senderInfo == null) {
            throw new IllegalArgumentException("Sender information is required");
        }
        if (recipientInfo == null) {
            throw new IllegalArgumentException("Recipient information is required");
        }
        if (referenceNumber == null || referenceNumber.isBlank()) {
            throw new IllegalArgumentException("Reference number is required");
        }

        // Validate value objects
        senderInfo.validate();
        recipientInfo.validate();
    }

    /**
     * Checks if the receipt has expired.
     *
     * @return true if current time is after expiry date
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    /**
     * Marks the receipt as expired.
     */
    public void markAsExpired() {
        this.status = ReceiptStatus.EXPIRED;
    }

    /**
     * Records an access to this receipt (updates access count and timestamp).
     */
    public void recordAccess() {
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * Converts this receipt to a shareable format with masked account numbers.
     *
     * @return ShareableReceipt value object
     */
    public ShareableReceipt toShareableFormat() {
        return ShareableReceipt.builder()
                .receiptId(this.id.toString())
                .transactionId(this.transactionId)
                .amount(this.amount)
                .currency(this.currency)
                .timestamp(this.timestamp)
                .referenceNumber(this.referenceNumber)
                .status(this.status)
                .senderName(this.senderInfo.getName())
                .senderAccountMasked(maskAccountNumber(this.senderInfo.getAccountNumber()))
                .senderBankName(this.senderInfo.getBankName())
                .recipientName(this.recipientInfo.getName())
                .recipientAccountMasked(maskAccountNumber(this.recipientInfo.getAccountNumber()))
                .recipientBankName(this.recipientInfo.getBankName())
                .build();
    }

    /**
     * Masks an account number, showing only the last 4 digits.
     *
     * @param accountNumber The full account number
     * @return Masked account number (e.g., "****5678")
     */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            return "****";
        }
        if (accountNumber.length() <= 4) {
            return "****" + accountNumber;
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    /**
     * Formats the amount with currency for display.
     *
     * @return Formatted amount (e.g., "IDR 1.500.000,00")
     */
    public String getFormattedAmount() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("id", "ID"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');

        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        String formattedNumber = formatter.format(this.amount);

        return this.currency + " " + formattedNumber;
    }

    /**
     * Formats the timestamp for display in Indonesian locale.
     *
     * @return Formatted timestamp (e.g., "01 Maret 2024, 14:30 WIB")
     */
    public String getFormattedTimestamp() {
        return this.timestamp.format(TIMESTAMP_FORMATTER);
    }

    /**
     * Gets the remaining days until expiry.
     *
     * @return Number of days until expiry (0 if already expired)
     */
    public long getDaysUntilExpiry() {
        if (isExpired()) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), expiryDate);
    }
}
