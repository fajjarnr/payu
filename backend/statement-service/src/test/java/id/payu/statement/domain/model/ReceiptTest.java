package id.payu.statement.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Domain tests for Receipt aggregate root (TDD - RED Phase)
 * Epic E-19: Transaction Proof & Receipts (IMP-055)
 */
@DisplayName("Receipt Domain Model Tests")
class ReceiptTest {

    private static final String TRANSACTION_ID = "TXN-20240301-ABC123";
    private static final BigDecimal AMOUNT = new BigDecimal("150000.00");
    private static final String CURRENCY = "IDR";
    private static final String REFERENCE_NUMBER = "REF-BIFAST-789456";

    private SenderInfo createSenderInfo() {
        return SenderInfo.builder()
                .name("John Doe")
                .accountNumber("1234567890")
                .bankName("PayU Digital Banking")
                .build();
    }

    private RecipientInfo createRecipientInfo() {
        return RecipientInfo.builder()
                .name("Jane Smith")
                .accountNumber("0987654321")
                .bankName("Bank Central Asia")
                .build();
    }

    @Test
    @DisplayName("Should create receipt with all required fields")
    void shouldCreateReceiptWithAllRequiredFields() {
        // Given
        SenderInfo sender = createSenderInfo();
        RecipientInfo recipient = createRecipientInfo();
        LocalDateTime timestamp = LocalDateTime.now();

        // When
        Receipt receipt = Receipt.builder()
                .id(UUID.randomUUID())
                .transactionId(TRANSACTION_ID)
                .amount(AMOUNT)
                .currency(CURRENCY)
                .senderInfo(sender)
                .recipientInfo(recipient)
                .timestamp(timestamp)
                .status(ReceiptStatus.GENERATED)
                .referenceNumber(REFERENCE_NUMBER)
                .build();

        // Then
        assertNotNull(receipt.getId());
        assertEquals(TRANSACTION_ID, receipt.getTransactionId());
        assertEquals(AMOUNT, receipt.getAmount());
        assertEquals(CURRENCY, receipt.getCurrency());
        assertEquals(sender, receipt.getSenderInfo());
        assertEquals(recipient, receipt.getRecipientInfo());
        assertEquals(timestamp, receipt.getTimestamp());
        assertEquals(ReceiptStatus.GENERATED, receipt.getStatus());
        assertEquals(REFERENCE_NUMBER, receipt.getReferenceNumber());
    }

    @Test
    @DisplayName("Should generate receipt with default status as GENERATED")
    void shouldGenerateReceiptWithDefaultStatusGenerated() {
        // Given
        SenderInfo sender = createSenderInfo();
        RecipientInfo recipient = createRecipientInfo();

        // When
        Receipt receipt = Receipt.generate(TRANSACTION_ID, AMOUNT, CURRENCY, sender, recipient, REFERENCE_NUMBER);

        // Then
        assertNotNull(receipt.getId());
        assertEquals(TRANSACTION_ID, receipt.getTransactionId());
        assertEquals(AMOUNT, receipt.getAmount());
        assertEquals(CURRENCY, receipt.getCurrency());
        assertEquals(sender, receipt.getSenderInfo());
        assertEquals(recipient, receipt.getRecipientInfo());
        assertEquals(ReceiptStatus.GENERATED, receipt.getStatus());
        assertEquals(REFERENCE_NUMBER, receipt.getReferenceNumber());
        assertNotNull(receipt.getTimestamp());
        assertNotNull(receipt.getExpiryDate());
    }

    @Test
    @DisplayName("Should set expiry date to 90 days from generation")
    void shouldSetExpiryDateTo90DaysFromGeneration() {
        // Given
        SenderInfo sender = createSenderInfo();
        RecipientInfo recipient = createRecipientInfo();
        LocalDateTime beforeGeneration = LocalDateTime.now();

        // When
        Receipt receipt = Receipt.generate(TRANSACTION_ID, AMOUNT, CURRENCY, sender, recipient, REFERENCE_NUMBER);
        LocalDateTime afterGeneration = LocalDateTime.now();

        // Then
        assertNotNull(receipt.getExpiryDate());
        // Expiry should be 90 days from generation
        LocalDateTime expectedExpiryMin = beforeGeneration.plusDays(90).minusSeconds(1);
        LocalDateTime expectedExpiryMax = afterGeneration.plusDays(90).plusSeconds(1);
        assertTrue(receipt.getExpiryDate().isAfter(expectedExpiryMin) || receipt.getExpiryDate().isEqual(expectedExpiryMin));
        assertTrue(receipt.getExpiryDate().isBefore(expectedExpiryMax) || receipt.getExpiryDate().isEqual(expectedExpiryMax));
    }

    @Test
    @DisplayName("Should return true for isExpired when current date is after expiry")
    void shouldReturnTrueForIsExpiredWhenAfterExpiry() {
        // Given
        SenderInfo sender = createSenderInfo();
        RecipientInfo recipient = createRecipientInfo();
        Receipt receipt = Receipt.generate(TRANSACTION_ID, AMOUNT, CURRENCY, sender, recipient, REFERENCE_NUMBER);

        // Manually set expiry to past date for testing
        Receipt expiredReceipt = Receipt.builder()
                .id(receipt.getId())
                .transactionId(receipt.getTransactionId())
                .amount(receipt.getAmount())
                .currency(receipt.getCurrency())
                .senderInfo(receipt.getSenderInfo())
                .recipientInfo(receipt.getRecipientInfo())
                .timestamp(receipt.getTimestamp())
                .status(ReceiptStatus.GENERATED)
                .referenceNumber(receipt.getReferenceNumber())
                .expiryDate(LocalDateTime.now().minusDays(1))
                .build();

        // When & Then
        assertTrue(expiredReceipt.isExpired());
    }

    @Test
    @DisplayName("Should return false for isExpired when current date is before expiry")
    void shouldReturnFalseForIsExpiredWhenBeforeExpiry() {
        // Given
        SenderInfo sender = createSenderInfo();
        RecipientInfo recipient = createRecipientInfo();
        Receipt receipt = Receipt.generate(TRANSACTION_ID, AMOUNT, CURRENCY, sender, recipient, REFERENCE_NUMBER);

        // When & Then
        assertFalse(receipt.isExpired());
    }

    @Test
    @DisplayName("Should mark receipt as expired")
    void shouldMarkReceiptAsExpired() {
        // Given
        SenderInfo sender = createSenderInfo();
        RecipientInfo recipient = createRecipientInfo();
        Receipt receipt = Receipt.generate(TRANSACTION_ID, AMOUNT, CURRENCY, sender, recipient, REFERENCE_NUMBER);

        // When
        receipt.markAsExpired();

        // Then
        assertEquals(ReceiptStatus.EXPIRED, receipt.getStatus());
    }

    @Test
    @DisplayName("Should convert to shareable format with masked account numbers")
    void shouldConvertToShareableFormatWithMaskedAccountNumbers() {
        // Given
        SenderInfo sender = createSenderInfo();
        RecipientInfo recipient = createRecipientInfo();
        Receipt receipt = Receipt.generate(TRANSACTION_ID, AMOUNT, CURRENCY, sender, recipient, REFERENCE_NUMBER);

        // When
        ShareableReceipt shareable = receipt.toShareableFormat();

        // Then
        assertNotNull(shareable);
        assertEquals(receipt.getId().toString(), shareable.getReceiptId());
        assertEquals(TRANSACTION_ID, shareable.getTransactionId());
        assertEquals(AMOUNT, shareable.getAmount());
        assertEquals(CURRENCY, shareable.getCurrency());
        assertEquals(receipt.getTimestamp(), shareable.getTimestamp());
        assertEquals(REFERENCE_NUMBER, shareable.getReferenceNumber());
        assertEquals(ReceiptStatus.GENERATED, shareable.getStatus());

        // Sender info should be masked
        assertEquals(sender.getName(), shareable.getSenderName());
        assertEquals("****7890", shareable.getSenderAccountMasked());
        assertEquals(sender.getBankName(), shareable.getSenderBankName());

        // Recipient info should be masked
        assertEquals(recipient.getName(), shareable.getRecipientName());
        assertEquals("****4321", shareable.getRecipientAccountMasked());
        assertEquals(recipient.getBankName(), shareable.getRecipientBankName());
    }

    @Test
    @DisplayName("Should validate amount is positive")
    void shouldValidateAmountIsPositive() {
        // Given
        SenderInfo sender = createSenderInfo();
        RecipientInfo recipient = createRecipientInfo();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                Receipt.generate(TRANSACTION_ID, BigDecimal.ZERO, CURRENCY, sender, recipient, REFERENCE_NUMBER)
        );
        assertEquals("Amount must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should validate transactionId is not blank")
    void shouldValidateTransactionIdIsNotBlank() {
        // Given
        SenderInfo sender = createSenderInfo();
        RecipientInfo recipient = createRecipientInfo();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                Receipt.generate("", AMOUNT, CURRENCY, sender, recipient, REFERENCE_NUMBER)
        );
        assertEquals("Transaction ID is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should validate sender info is not null")
    void shouldValidateSenderInfoIsNotNull() {
        // Given
        RecipientInfo recipient = createRecipientInfo();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                Receipt.generate(TRANSACTION_ID, AMOUNT, CURRENCY, null, recipient, REFERENCE_NUMBER)
        );
        assertEquals("Sender information is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should validate recipient info is not null")
    void shouldValidateRecipientInfoIsNotNull() {
        // Given
        SenderInfo sender = createSenderInfo();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                Receipt.generate(TRANSACTION_ID, AMOUNT, CURRENCY, sender, null, REFERENCE_NUMBER)
        );
        assertEquals("Recipient information is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should validate reference number is not blank")
    void shouldValidateReferenceNumberIsNotBlank() {
        // Given
        SenderInfo sender = createSenderInfo();
        RecipientInfo recipient = createRecipientInfo();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                Receipt.generate(TRANSACTION_ID, AMOUNT, CURRENCY, sender, recipient, "")
        );
        assertEquals("Reference number is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should mask account number correctly")
    void shouldMaskAccountNumberCorrectly() {
        // Given & When & Then
        assertEquals("****5678", Receipt.maskAccountNumber("12345678"));
        assertEquals("****90", Receipt.maskAccountNumber("123490"));
        assertEquals("****1", Receipt.maskAccountNumber("1"));
        assertEquals("****", Receipt.maskAccountNumber(""));
    }

    @Test
    @DisplayName("Should generate unique receipt IDs")
    void shouldGenerateUniqueReceiptIds() {
        // Given
        SenderInfo sender = createSenderInfo();
        RecipientInfo recipient = createRecipientInfo();

        // When
        Receipt receipt1 = Receipt.generate(TRANSACTION_ID + "1", AMOUNT, CURRENCY, sender, recipient, REFERENCE_NUMBER);
        Receipt receipt2 = Receipt.generate(TRANSACTION_ID + "2", AMOUNT, CURRENCY, sender, recipient, REFERENCE_NUMBER);

        // Then
        assertNotEquals(receipt1.getId(), receipt2.getId());
    }

    @Test
    @DisplayName("Should update access count when accessed")
    void shouldUpdateAccessCountWhenAccessed() {
        // Given
        SenderInfo sender = createSenderInfo();
        RecipientInfo recipient = createRecipientInfo();
        Receipt receipt = Receipt.generate(TRANSACTION_ID, AMOUNT, CURRENCY, sender, recipient, REFERENCE_NUMBER);

        // When
        receipt.recordAccess();
        receipt.recordAccess();

        // Then
        assertEquals(2, receipt.getAccessCount());
        assertNotNull(receipt.getLastAccessedAt());
    }

    @Test
    @DisplayName("Should return formatted amount with currency")
    void shouldReturnFormattedAmountWithCurrency() {
        // Given
        SenderInfo sender = createSenderInfo();
        RecipientInfo recipient = createRecipientInfo();
        Receipt receipt = Receipt.generate(TRANSACTION_ID, new BigDecimal("1500000.50"), CURRENCY, sender, recipient, REFERENCE_NUMBER);

        // When
        String formattedAmount = receipt.getFormattedAmount();

        // Then
        assertEquals("IDR 1.500.000,50", formattedAmount);
    }

    @Test
    @DisplayName("Should return formatted timestamp")
    void shouldReturnFormattedTimestamp() {
        // Given
        SenderInfo sender = createSenderInfo();
        RecipientInfo recipient = createRecipientInfo();
        LocalDateTime timestamp = LocalDateTime.of(2024, 3, 1, 14, 30, 0);

        Receipt receipt = Receipt.builder()
                .id(UUID.randomUUID())
                .transactionId(TRANSACTION_ID)
                .amount(AMOUNT)
                .currency(CURRENCY)
                .senderInfo(sender)
                .recipientInfo(recipient)
                .timestamp(timestamp)
                .status(ReceiptStatus.GENERATED)
                .referenceNumber(REFERENCE_NUMBER)
                .expiryDate(timestamp.plusDays(90))
                .build();

        // When
        String formattedTimestamp = receipt.getFormattedTimestamp();

        // Then
        assertEquals("01 Maret 2024, 14:30 WIB", formattedTimestamp);
    }
}
