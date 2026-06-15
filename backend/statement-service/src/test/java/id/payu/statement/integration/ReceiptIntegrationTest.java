package id.payu.statement.integration;

import id.payu.statement.adapter.persistence.entity.ReceiptEntity;
import id.payu.statement.adapter.persistence.repository.ReceiptJpaRepository;
import id.payu.statement.application.service.dto.ReceiptGenerationRequest;
import id.payu.statement.application.service.dto.ReceiptResponse;
import id.payu.statement.domain.model.ReceiptStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.restclient.test.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Receipt API endpoints.
 * Uses Testcontainers for database isolation.
 * Epic E-19: Transaction Proof & Receipts (IMP-055)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = TestContainersConfig.Initializer.class)
@ActiveProfiles("integration-test")
@Tag("integration")
@DisplayName("Receipt Integration Tests")
class ReceiptIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ReceiptJpaRepository receiptJpaRepository;

    private String authToken;

    @BeforeEach
    void setUp() {
        // Clear receipts before each test
        receiptJpaRepository.deleteAll();

        // Setup mock JWT token for authentication
        authToken = "Bearer mock-jwt-token";
    }

    @Test
    @DisplayName("Should generate receipt for transaction")
    void shouldGenerateReceiptForTransaction() {
        // Given
        ReceiptGenerationRequest request = ReceiptGenerationRequest.builder()
                .transactionId("TXN-INT-001")
                .customerId("CUST-001")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", authToken);
        HttpEntity<ReceiptGenerationRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<ReceiptResponse> response = restTemplate.postForEntity(
                "/api/v1/statements/receipts/generate",
                entity,
                ReceiptResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TXN-INT-001", response.getBody().getTransactionId());
        assertNotNull(response.getBody().getReceiptId());
        assertEquals(ReceiptStatus.GENERATED, response.getBody().getStatus());

        // Verify in database
        assertTrue(receiptJpaRepository.existsByTransactionId("TXN-INT-001"));
    }

    @Test
    @DisplayName("Should return existing receipt when regenerating")
    void shouldReturnExistingReceiptWhenRegenerating() {
        // Given - Create existing receipt
        ReceiptEntity existingReceipt = ReceiptEntity.builder()
                .id(UUID.randomUUID())
                .transactionId("TXN-INT-002")
                .amount(new BigDecimal("250000.00"))
                .currency("IDR")
                .senderName("Test Sender")
                .senderAccountNumber("1111111111")
                .senderBankName("PayU")
                .recipientName("Test Recipient")
                .recipientAccountNumber("2222222222")
                .recipientBankName("BCA")
                .transactionTimestamp(LocalDateTime.now())
                .status(ReceiptStatus.GENERATED)
                .referenceNumber("REF-TEST-002")
                .expiryDate(LocalDateTime.now().plusDays(90))
                .accessCount(0)
                .build();
        receiptJpaRepository.save(existingReceipt);

        ReceiptGenerationRequest request = ReceiptGenerationRequest.builder()
                .transactionId("TXN-INT-002")
                .customerId("CUST-001")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", authToken);
        HttpEntity<ReceiptGenerationRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<ReceiptResponse> response = restTemplate.postForEntity(
                "/api/v1/statements/receipts/generate",
                entity,
                ReceiptResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(existingReceipt.getId(), response.getBody().getReceiptId());

        // Verify access count was updated
        ReceiptEntity updated = receiptJpaRepository.findByTransactionId("TXN-INT-002").orElseThrow();
        assertEquals(1, updated.getAccessCount());
    }

    @Test
    @DisplayName("Should get receipt by ID")
    void shouldGetReceiptById() {
        // Given
        UUID receiptId = UUID.randomUUID();
        ReceiptEntity receipt = ReceiptEntity.builder()
                .id(receiptId)
                .transactionId("TXN-INT-003")
                .amount(new BigDecimal("500000.00"))
                .currency("IDR")
                .senderName("Alice")
                .senderAccountNumber("3333333333")
                .senderBankName("PayU")
                .recipientName("Bob")
                .recipientAccountNumber("4444444444")
                .recipientBankName("Mandiri")
                .transactionTimestamp(LocalDateTime.now())
                .status(ReceiptStatus.GENERATED)
                .referenceNumber("REF-TEST-003")
                .expiryDate(LocalDateTime.now().plusDays(90))
                .accessCount(0)
                .build();
        receiptJpaRepository.save(receipt);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<ReceiptResponse> response = restTemplate.exchange(
                "/api/v1/statements/receipts/" + receiptId,
                HttpMethod.GET,
                entity,
                ReceiptResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(receiptId, response.getBody().getReceiptId());
        assertEquals("TXN-INT-003", response.getBody().getTransactionId());
        assertEquals("Alice", response.getBody().getSenderName());
        assertEquals("Bob", response.getBody().getRecipientName());
    }

    @Test
    @DisplayName("Should return 404 for non-existent receipt")
    void shouldReturn404ForNonExistentReceipt() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/statements/receipts/" + nonExistentId,
                HttpMethod.GET,
                entity,
                String.class
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("Should get receipt by transaction ID")
    void shouldGetReceiptByTransactionId() {
        // Given
        UUID receiptId = UUID.randomUUID();
        ReceiptEntity receipt = ReceiptEntity.builder()
                .id(receiptId)
                .transactionId("TXN-INT-004")
                .amount(new BigDecimal("750000.00"))
                .currency("IDR")
                .senderName("Charlie")
                .senderAccountNumber("5555555555")
                .senderBankName("PayU")
                .recipientName("David")
                .recipientAccountNumber("6666666666")
                .recipientBankName("BNI")
                .transactionTimestamp(LocalDateTime.now())
                .status(ReceiptStatus.GENERATED)
                .referenceNumber("REF-TEST-004")
                .expiryDate(LocalDateTime.now().plusDays(90))
                .accessCount(0)
                .build();
        receiptJpaRepository.save(receipt);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<ReceiptResponse> response = restTemplate.exchange(
                "/api/v1/statements/receipts/transaction/TXN-INT-004",
                HttpMethod.GET,
                entity,
                ReceiptResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(receiptId, response.getBody().getReceiptId());
        assertEquals("Charlie", response.getBody().getSenderName());
    }

    @Test
    @DisplayName("Should download receipt PDF")
    void shouldDownloadReceiptPdf() {
        // Given
        UUID receiptId = UUID.randomUUID();
        ReceiptEntity receipt = ReceiptEntity.builder()
                .id(receiptId)
                .transactionId("TXN-INT-005")
                .amount(new BigDecimal("1000000.00"))
                .currency("IDR")
                .senderName("Eve")
                .senderAccountNumber("7777777777")
                .senderBankName("PayU")
                .recipientName("Frank")
                .recipientAccountNumber("8888888888")
                .recipientBankName("BRI")
                .transactionTimestamp(LocalDateTime.now())
                .status(ReceiptStatus.GENERATED)
                .referenceNumber("REF-TEST-005")
                .expiryDate(LocalDateTime.now().plusDays(90))
                .accessCount(0)
                .build();
        receiptJpaRepository.save(receipt);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<byte[]> response = restTemplate.exchange(
                "/api/v1/statements/receipts/" + receiptId + "/download",
                HttpMethod.GET,
                entity,
                byte[].class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());

        // Verify access count was updated
        ReceiptEntity updated = receiptJpaRepository.findById(receiptId).orElseThrow();
        assertEquals(1, updated.getAccessCount());
        assertNotNull(updated.getLastAccessedAt());
    }

    @Test
    @DisplayName("Should return 410 for expired receipt PDF download")
    void shouldReturn410ForExpiredReceiptPdfDownload() {
        // Given
        UUID receiptId = UUID.randomUUID();
        ReceiptEntity expiredReceipt = ReceiptEntity.builder()
                .id(receiptId)
                .transactionId("TXN-INT-006")
                .amount(new BigDecimal("200000.00"))
                .currency("IDR")
                .senderName("Grace")
                .senderAccountNumber("9999999999")
                .senderBankName("PayU")
                .recipientName("Henry")
                .recipientAccountNumber("0000000000")
                .recipientBankName("CIMB")
                .transactionTimestamp(LocalDateTime.now().minusDays(100))
                .status(ReceiptStatus.GENERATED)
                .referenceNumber("REF-TEST-006")
                .expiryDate(LocalDateTime.now().minusDays(10)) // Expired
                .accessCount(0)
                .build();
        receiptJpaRepository.save(expiredReceipt);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/statements/receipts/" + receiptId + "/download",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Then
        assertEquals(HttpStatus.GONE, response.getStatusCode());
    }

    @Test
    @DisplayName("Should return masked account numbers")
    void shouldReturnMaskedAccountNumbers() {
        // Given
        UUID receiptId = UUID.randomUUID();
        ReceiptEntity receipt = ReceiptEntity.builder()
                .id(receiptId)
                .transactionId("TXN-INT-007")
                .amount(new BigDecimal("300000.00"))
                .currency("IDR")
                .senderName("Ivan")
                .senderAccountNumber("1234567890")
                .senderBankName("PayU")
                .recipientName("Judy")
                .recipientAccountNumber("0987654321")
                .recipientBankName("BCA")
                .transactionTimestamp(LocalDateTime.now())
                .status(ReceiptStatus.GENERATED)
                .referenceNumber("REF-TEST-007")
                .expiryDate(LocalDateTime.now().plusDays(90))
                .accessCount(0)
                .build();
        receiptJpaRepository.save(receipt);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // When
        ResponseEntity<ReceiptResponse> response = restTemplate.exchange(
                "/api/v1/statements/receipts/" + receiptId,
                HttpMethod.GET,
                entity,
                ReceiptResponse.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("****7890", response.getBody().getSenderAccountMasked());
        assertEquals("****4321", response.getBody().getRecipientAccountMasked());
    }
}
