package id.payu.statement.application.service;

import id.payu.statement.adapter.client.TransactionServiceClient;
import id.payu.statement.domain.port.out.ReceiptRepositoryPort;
import id.payu.statement.application.service.dto.ReceiptGenerationRequest;
import id.payu.statement.application.service.dto.ReceiptResponse;
import id.payu.statement.domain.model.ReceiptException;
import id.payu.statement.domain.model.Receipt;
import id.payu.statement.domain.model.ReceiptStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReceiptService.
 * Epic E-19: Transaction Proof & Receipts (IMP-055)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReceiptService Tests")
class ReceiptServiceTest {

    @Mock
    private ReceiptRepositoryPort receiptRepository;

    @Mock
    private TransactionServiceClient transactionServiceClient;

    @InjectMocks
    private ReceiptService receiptService;

    private static final String TRANSACTION_ID = "TXN-20240301-ABC123";
    private static final String CUSTOMER_ID = "CUST-001";
    private static final UUID RECEIPT_ID = UUID.randomUUID();

    private Receipt createTestReceipt() {
        return Receipt.builder()
                .id(RECEIPT_ID)
                .transactionId(TRANSACTION_ID)
                .customerId(CUSTOMER_ID)
                .amount(new BigDecimal("150000.00"))
                .currency("IDR")
                .senderInfo(new id.payu.statement.domain.model.SenderInfo("John Doe", "1234567890", "PayU Digital Banking"))
                .recipientInfo(new id.payu.statement.domain.model.RecipientInfo("Jane Smith", "0987654321", "Bank Central Asia"))
                .timestamp(LocalDateTime.now())
                .status(ReceiptStatus.GENERATED)
                .referenceNumber("REF-BIFAST-789456")
                .expiryDate(LocalDateTime.now().plusDays(90))
                .accessCount(0)
                .build();
    }

    @BeforeEach
    void setUp() {
        // Inject @Value fields that MockitoExtension doesn't populate
        ReflectionTestUtils.setField(receiptService, "companyName", "PayU Digital Banking");
        ReflectionTestUtils.setField(receiptService, "companyLogoUrl", "https://payu.fajjjar.my.id/logo.png");
        ReflectionTestUtils.setField(receiptService, "supportPhone", "+62 21 555-1234");
        ReflectionTestUtils.setField(receiptService, "supportEmail", "support@payu.fajjjar.my.id");
    }

    @Test
    @DisplayName("Should return existing receipt when already exists")
    void shouldReturnExistingReceiptWhenAlreadyExists() {
        // Given
        Receipt existingReceipt = createTestReceipt();
        ReceiptGenerationRequest request = ReceiptGenerationRequest.builder()
                .transactionId(TRANSACTION_ID)
                .customerId(CUSTOMER_ID)
                .build();

        when(receiptRepository.findByTransactionId(TRANSACTION_ID))
                .thenReturn(Optional.of(existingReceipt));
        when(receiptRepository.save(any(Receipt.class))).thenReturn(existingReceipt);

        // When
        ReceiptResponse response = receiptService.generateReceipt(request);

        // Then
        assertNotNull(response);
        assertEquals(RECEIPT_ID, response.getReceiptId());
        assertEquals(TRANSACTION_ID, response.getTransactionId());
        verify(receiptRepository).findByTransactionId(TRANSACTION_ID);
        verify(receiptRepository).save(any(Receipt.class)); // For access count update
    }

    @Test
    @DisplayName("Should generate new receipt when not exists")
    void shouldGenerateNewReceiptWhenNotExists() {
        // Given
        ReceiptGenerationRequest request = ReceiptGenerationRequest.builder()
                .transactionId(TRANSACTION_ID)
                .customerId(CUSTOMER_ID)
                .build();

        Receipt newReceipt = createTestReceipt();

        when(receiptRepository.findByTransactionId(TRANSACTION_ID))
                .thenReturn(Optional.empty());
        when(receiptRepository.save(any(Receipt.class))).thenReturn(newReceipt);

        // When
        ReceiptResponse response = receiptService.generateReceipt(request);

        // Then
        assertNotNull(response);
        assertEquals(TRANSACTION_ID, response.getTransactionId());
        assertEquals("IDR", response.getCurrency());
        assertEquals("John Doe", response.getSenderName());
        assertEquals("Jane Smith", response.getRecipientName());
        verify(receiptRepository).findByTransactionId(TRANSACTION_ID);
        verify(receiptRepository, times(1)).save(any(Receipt.class));
    }

    @Test
    @DisplayName("Should get receipt by ID successfully")
    void shouldGetReceiptByIdSuccessfully() {
        // Given
        Receipt receipt = createTestReceipt();
        when(receiptRepository.findById(RECEIPT_ID)).thenReturn(Optional.of(receipt));

        // When
        ReceiptResponse response = receiptService.getReceipt(RECEIPT_ID, CUSTOMER_ID);

        // Then
        assertNotNull(response);
        assertEquals(RECEIPT_ID, response.getReceiptId());
        assertEquals(TRANSACTION_ID, response.getTransactionId());
        assertFalse(response.isExpired());
    }

    @Test
    @DisplayName("Should throw exception when receipt not found by ID")
    void shouldThrowExceptionWhenReceiptNotFoundById() {
        // Given
        when(receiptRepository.findById(RECEIPT_ID)).thenReturn(Optional.empty());

        // When & Then
        ReceiptException exception = assertThrows(ReceiptException.class, () ->
                receiptService.getReceipt(RECEIPT_ID, CUSTOMER_ID)
        );
        assertEquals("RECEIPT_002", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Receipt not found"));
    }

    @Test
    @DisplayName("Should get receipt by transaction ID")
    void shouldGetReceiptByTransactionId() {
        // Given
        Receipt receipt = createTestReceipt();
        when(receiptRepository.findByTransactionId(TRANSACTION_ID))
                .thenReturn(Optional.of(receipt));

        // When
        Optional<ReceiptResponse> response = receiptService.getReceiptByTransactionId(TRANSACTION_ID, CUSTOMER_ID);

        // Then
        assertTrue(response.isPresent());
        assertEquals(RECEIPT_ID, response.get().getReceiptId());
    }

    @Test
    @DisplayName("Should return empty optional when receipt not found by transaction ID")
    void shouldReturnEmptyOptionalWhenReceiptNotFoundByTransactionId() {
        // Given
        when(receiptRepository.findByTransactionId(TRANSACTION_ID))
                .thenReturn(Optional.empty());

        // When
        Optional<ReceiptResponse> response = receiptService.getReceiptByTransactionId(TRANSACTION_ID, CUSTOMER_ID);

        // Then
        assertTrue(response.isEmpty());
    }

    @Test
    @DisplayName("Should generate PDF for valid receipt")
    void shouldGeneratePdfForValidReceipt() {
        // Given
        Receipt receipt = createTestReceipt();
        when(receiptRepository.findById(RECEIPT_ID)).thenReturn(Optional.of(receipt));
        when(receiptRepository.save(any(Receipt.class))).thenReturn(receipt);

        // When
        byte[] pdfBytes = receiptService.generatePdf(RECEIPT_ID, CUSTOMER_ID);

        // Then
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        verify(receiptRepository).findById(RECEIPT_ID);
        verify(receiptRepository).save(any(Receipt.class)); // Access count update
    }

    @Test
    @DisplayName("Should throw exception when generating PDF for expired receipt")
    void shouldThrowExceptionWhenGeneratingPdfForExpiredReceipt() {
        // Given
        Receipt expiredReceipt = Receipt.builder()
                .id(RECEIPT_ID)
                .transactionId(TRANSACTION_ID)
                .customerId(CUSTOMER_ID)
                .amount(new BigDecimal("150000.00"))
                .currency("IDR")
                .senderInfo(new id.payu.statement.domain.model.SenderInfo("John Doe", "1234567890", "PayU Digital Banking"))
                .recipientInfo(new id.payu.statement.domain.model.RecipientInfo("Jane Smith", "0987654321", "Bank Central Asia"))
                .timestamp(LocalDateTime.now().minusDays(100))
                .status(ReceiptStatus.GENERATED)
                .referenceNumber("REF-123")
                .expiryDate(LocalDateTime.now().minusDays(10)) // Expired
                .build();

        when(receiptRepository.findById(RECEIPT_ID)).thenReturn(Optional.of(expiredReceipt));

        // When & Then
        ReceiptException exception = assertThrows(ReceiptException.class, () ->
                receiptService.generatePdf(RECEIPT_ID, CUSTOMER_ID)
        );
        assertEquals("RECEIPT_003", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("expired"));
    }

    @Test
    @DisplayName("Should generate PDF by transaction ID")
    void shouldGeneratePdfByTransactionId() {
        // Given
        Receipt receipt = createTestReceipt();
        when(receiptRepository.findByTransactionId(TRANSACTION_ID))
                .thenReturn(Optional.of(receipt));
        when(receiptRepository.findById(RECEIPT_ID)).thenReturn(Optional.of(receipt));
        when(receiptRepository.save(any(Receipt.class))).thenReturn(receipt);

        // When
        byte[] pdfBytes = receiptService.generatePdfByTransactionId(TRANSACTION_ID, CUSTOMER_ID);

        // Then
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    @DisplayName("Should throw exception when generating PDF for non-existent transaction")
    void shouldThrowExceptionWhenGeneratingPdfForNonExistentTransaction() {
        // Given
        when(receiptRepository.findByTransactionId(TRANSACTION_ID))
                .thenReturn(Optional.empty());

        // When & Then
        ReceiptException exception = assertThrows(ReceiptException.class, () ->
                receiptService.generatePdfByTransactionId(TRANSACTION_ID, CUSTOMER_ID)
        );
        assertEquals("RECEIPT_002", exception.getErrorCode());
    }

    @Test
    @DisplayName("Should mark receipt as expired when expiry date passed")
    void shouldMarkReceiptAsExpiredWhenExpiryDatePassed() {
        // Given
        Receipt expiredReceipt = Receipt.builder()
                .id(RECEIPT_ID)
                .transactionId(TRANSACTION_ID)
                .customerId(CUSTOMER_ID)
                .amount(new BigDecimal("150000.00"))
                .currency("IDR")
                .senderInfo(new id.payu.statement.domain.model.SenderInfo("John Doe", "1234567890", "PayU Digital Banking"))
                .recipientInfo(new id.payu.statement.domain.model.RecipientInfo("Jane Smith", "0987654321", "Bank Central Asia"))
                .timestamp(LocalDateTime.now().minusDays(100))
                .status(ReceiptStatus.GENERATED)
                .referenceNumber("REF-123")
                .expiryDate(LocalDateTime.now().minusDays(10)) // Expired
                .build();

        when(receiptRepository.findById(RECEIPT_ID)).thenReturn(Optional.of(expiredReceipt));
        when(receiptRepository.save(any(Receipt.class))).thenReturn(expiredReceipt);

        // When
        ReceiptResponse response = receiptService.getReceipt(RECEIPT_ID, CUSTOMER_ID);

        // Then
        assertTrue(response.isExpired());
        assertEquals(0, response.getDaysUntilExpiry());
        verify(receiptRepository).save(any(Receipt.class)); // Status update
    }

    @Test
    @DisplayName("Should return formatted amount in response")
    void shouldReturnFormattedAmountInResponse() {
        // Given
        Receipt receipt = createTestReceipt();
        when(receiptRepository.findById(RECEIPT_ID)).thenReturn(Optional.of(receipt));

        // When
        ReceiptResponse response = receiptService.getReceipt(RECEIPT_ID, CUSTOMER_ID);

        // Then
        assertNotNull(response.getFormattedAmount());
        assertTrue(response.getFormattedAmount().startsWith("IDR"));
        assertTrue(response.getFormattedAmount().contains("150.000"));
    }

    @Test
    @DisplayName("Should return formatted timestamp in response")
    void shouldReturnFormattedTimestampInResponse() {
        // Given
        Receipt receipt = createTestReceipt();
        when(receiptRepository.findById(RECEIPT_ID)).thenReturn(Optional.of(receipt));

        // When
        ReceiptResponse response = receiptService.getReceipt(RECEIPT_ID, CUSTOMER_ID);

        // Then
        assertNotNull(response.getFormattedTimestamp());
        assertTrue(response.getFormattedTimestamp().contains("WIB"));
    }

    @Test
    @DisplayName("Should mask account numbers in response")
    void shouldMaskAccountNumbersInResponse() {
        // Given
        Receipt receipt = createTestReceipt();
        when(receiptRepository.findById(RECEIPT_ID)).thenReturn(Optional.of(receipt));

        // When
        ReceiptResponse response = receiptService.getReceipt(RECEIPT_ID, CUSTOMER_ID);

        // Then
        assertEquals("****7890", response.getSenderAccountMasked());
        assertEquals("****4321", response.getRecipientAccountMasked());
    }

    @Test
    @DisplayName("Should update access count when generating PDF")
    void shouldUpdateAccessCountWhenGeneratingPdf() {
        // Given
        Receipt receipt = createTestReceipt();
        when(receiptRepository.findById(RECEIPT_ID)).thenReturn(Optional.of(receipt));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        receiptService.generatePdf(RECEIPT_ID, CUSTOMER_ID);

        // Then
        verify(receiptRepository).save(argThat(savedReceipt ->
                savedReceipt.getAccessCount() == 1 && savedReceipt.getLastAccessedAt() != null
        ));
    }
}
