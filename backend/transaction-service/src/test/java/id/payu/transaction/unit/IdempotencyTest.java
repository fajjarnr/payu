package id.payu.transaction.unit;

import id.payu.transaction.application.service.TransactionService;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.out.*;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.dto.InitiateTransferResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyTest {

    @Mock
    private TransactionPersistencePort transactionPersistencePort;
    @Mock
    private WalletServicePort walletServicePort;
    @Mock
    private BifastServicePort bifastServicePort;
    @Mock
    private SknServicePort sknServicePort;
    @Mock
    private RgsServicePort rgsServicePort;
    @Mock
    private QrisServicePort qrisServicePort;
    @Mock
    private TransactionEventPublisherPort eventPublisherPort;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("should return existing transaction when idempotency key exists")
    void shouldReturnExistingTransaction_WhenIdempotencyKeyExists() {
        // Given
        String idempotencyKey = "key-123";
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("123456")
                .amount(new BigDecimal("50000"))
                .description("Test")
                .type(InitiateTransferRequest.TransactionType.INTERNAL_TRANSFER)
                .idempotencyKey(idempotencyKey)
                .build();

        Transaction existingTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .referenceNumber("TXN-EXISTING")
                .idempotencyKey(idempotencyKey)
                .status(Transaction.TransactionStatus.PENDING)
                .type(Transaction.TransactionType.INTERNAL_TRANSFER)
                .amount(new BigDecimal("50000"))
                .currency("IDR")
                .build();

        when(transactionPersistencePort.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingTransaction));

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(request);

        // Then
        assertThat(response.getReferenceNumber()).isEqualTo("TXN-EXISTING");
        verify(walletServicePort, never()).reserveBalance(any(), any(), any());
        verify(transactionPersistencePort, never()).save(any());
    }

    @Test
    @DisplayName("should create new transaction when idempotency key does not exist")
    void shouldCreateNewTransaction_WhenIdempotencyKeyDoesNotExist() {
        // Given
        String idempotencyKey = "key-new-123";
        UUID accountId = UUID.randomUUID();

        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(accountId)
                .recipientAccountNumber("123456")
                .amount(new BigDecimal("50000"))
                .description("New transaction with idempotency")
                .type(InitiateTransferRequest.TransactionType.INTERNAL_TRANSFER)
                .idempotencyKey(idempotencyKey)
                .build();

        when(transactionPersistencePort.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(transactionPersistencePort.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);
        when(walletServicePort.reserveBalance(any(), any(), any())).thenReturn(
                id.payu.transaction.dto.ReserveBalanceResponse.builder()
                        .reservationId("res-123")
                        .status("RESERVED")
                        .build()
        );

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(request);

        // Then
        assertThat(response).isNotNull();
        verify(walletServicePort, times(1)).reserveBalance(any(), any(), any());
        verify(transactionPersistencePort, times(2)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("should return failed transaction when idempotency key exists for failed transaction")
    void shouldReturnFailedTransaction_WhenIdempotencyKeyExistsForFailedTransaction() {
        // Given
        String idempotencyKey = "key-failed-123";

        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("123456")
                .amount(new BigDecimal("50000"))
                .description("Retry failed transaction")
                .type(InitiateTransferRequest.TransactionType.BIFAST_TRANSFER)
                .idempotencyKey(idempotencyKey)
                .build();

        Transaction failedTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .referenceNumber("TXN-FAILED")
                .idempotencyKey(idempotencyKey)
                .status(Transaction.TransactionStatus.FAILED)
                .type(Transaction.TransactionType.BIFAST_TRANSFER)
                .amount(new BigDecimal("50000"))
                .currency("IDR")
                .failureReason("BI-FAST Timeout")
                .build();

        when(transactionPersistencePort.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(failedTransaction));

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(request);

        // Then
        assertThat(response.getReferenceNumber()).isEqualTo("TXN-FAILED");
        assertThat(response.getStatus()).isEqualTo("FAILED");
        verify(walletServicePort, never()).reserveBalance(any(), any(), any());
    }

    @Test
    @DisplayName("should allow retry for different idempotency key with same parameters")
    void shouldAllowRetry_WithDifferentIdempotencyKey() {
        // Given
        UUID accountId = UUID.randomUUID();
        String firstKey = "key-first-123";
        String secondKey = "key-second-456";

        InitiateTransferRequest firstRequest = InitiateTransferRequest.builder()
                .senderAccountId(accountId)
                .recipientAccountNumber("123456")
                .amount(new BigDecimal("50000"))
                .description("First attempt")
                .type(InitiateTransferRequest.TransactionType.INTERNAL_TRANSFER)
                .idempotencyKey(firstKey)
                .build();

        InitiateTransferRequest secondRequest = InitiateTransferRequest.builder()
                .senderAccountId(accountId)
                .recipientAccountNumber("123456")
                .amount(new BigDecimal("50000"))
                .description("Second attempt with new key")
                .type(InitiateTransferRequest.TransactionType.INTERNAL_TRANSFER)
                .idempotencyKey(secondKey)
                .build();

        when(transactionPersistencePort.findByIdempotencyKey(firstKey))
                .thenReturn(Optional.empty());
        when(transactionPersistencePort.findByIdempotencyKey(secondKey))
                .thenReturn(Optional.empty());
        when(transactionPersistencePort.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);
        when(walletServicePort.reserveBalance(any(), any(), any())).thenReturn(
                id.payu.transaction.dto.ReserveBalanceResponse.builder()
                        .reservationId("res-123")
                        .status("RESERVED")
                        .build()
        );

        // When
        transactionService.initiateTransfer(firstRequest);
        transactionService.initiateTransfer(secondRequest);

        // Then
        verify(transactionPersistencePort, times(4)).save(any(Transaction.class));
        verify(walletServicePort, times(2)).reserveBalance(any(), any(), any());
    }

    @Test
    @DisplayName("should handle idempotency key for BI-FAST transfer")
    void shouldHandleIdempotencyKey_ForBifastTransfer() {
        // Given
        String idempotencyKey = "key-bifast-123";

        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("123456")
                .amount(new BigDecimal("50000"))
                .description("BI-FAST with idempotency")
                .type(InitiateTransferRequest.TransactionType.BIFAST_TRANSFER)
                .idempotencyKey(idempotencyKey)
                .build();

        Transaction existingBifastTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .referenceNumber("TXN-BIFAST-EXISTING")
                .idempotencyKey(idempotencyKey)
                .status(Transaction.TransactionStatus.PENDING)
                .type(Transaction.TransactionType.BIFAST_TRANSFER)
                .amount(new BigDecimal("50000"))
                .currency("IDR")
                .description("BI-FAST Transfer")
                .metadata("{\"externalTransactionId\":\"BIFAST-EXT-123\"}")
                .build();

        when(transactionPersistencePort.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingBifastTransaction));

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(request);

        // Then
        assertThat(response.getReferenceNumber()).isEqualTo("TXN-BIFAST-EXISTING");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        verify(walletServicePort, never()).reserveBalance(any(), any(), any());
    }

    @Test
    @DisplayName("should handle null idempotency key by always creating new transaction")
    void shouldCreateNewTransaction_WhenIdempotencyKeyIsNull() {
        // Given
        UUID accountId = UUID.randomUUID();

        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(accountId)
                .recipientAccountNumber("123456")
                .amount(new BigDecimal("50000"))
                .description("Transaction without idempotency key")
                .type(InitiateTransferRequest.TransactionType.INTERNAL_TRANSFER)
                .idempotencyKey(null)
                .build();

        when(transactionPersistencePort.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);
        when(walletServicePort.reserveBalance(any(), any(), any())).thenReturn(
                id.payu.transaction.dto.ReserveBalanceResponse.builder()
                        .reservationId("res-123")
                        .status("RESERVED")
                        .build()
        );

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(request);

        // Then
        assertThat(response).isNotNull();
        verify(transactionPersistencePort, atLeast(1)).save(any(Transaction.class));
        verify(walletServicePort).reserveBalance(any(), any(), any());
    }

    @Test
    @DisplayName("should handle idempotency key with completed transaction")
    void shouldReturnCompletedTransaction_WhenIdempotencyKeyExistsForCompletedTransaction() {
        // Given
        String idempotencyKey = "key-completed-123";

        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("123456")
                .amount(new BigDecimal("50000"))
                .description("Check completed transaction")
                .type(InitiateTransferRequest.TransactionType.INTERNAL_TRANSFER)
                .idempotencyKey(idempotencyKey)
                .build();

        Transaction completedTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .referenceNumber("TXN-COMPLETED")
                .idempotencyKey(idempotencyKey)
                .status(Transaction.TransactionStatus.COMPLETED)
                .type(Transaction.TransactionType.INTERNAL_TRANSFER)
                .amount(new BigDecimal("50000"))
                .currency("IDR")
                .build();

        when(transactionPersistencePort.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(completedTransaction));

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(request);

        // Then
        assertThat(response.getReferenceNumber()).isEqualTo("TXN-COMPLETED");
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        verify(walletServicePort, never()).reserveBalance(any(), any(), any());
        verify(transactionPersistencePort, never()).save(any());
    }

    @Test
    @DisplayName("should store idempotency key with new transaction")
    void shouldStoreIdempotencyKey_WithNewTransaction() {
        // Given
        String idempotencyKey = "key-store-123";
        UUID accountId = UUID.randomUUID();

        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(accountId)
                .recipientAccountNumber("123456")
                .amount(new BigDecimal("50000"))
                .description("Store idempotency key")
                .type(InitiateTransferRequest.TransactionType.INTERNAL_TRANSFER)
                .idempotencyKey(idempotencyKey)
                .build();

        when(transactionPersistencePort.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(transactionPersistencePort.save(any(Transaction.class))).thenAnswer(i -> {
            Transaction t = (Transaction) i.getArguments()[0];
            assertThat(t.getIdempotencyKey()).isEqualTo(idempotencyKey);
            return t;
        });
        when(walletServicePort.reserveBalance(any(), any(), any())).thenReturn(
                id.payu.transaction.dto.ReserveBalanceResponse.builder()
                        .reservationId("res-123")
                        .status("RESERVED")
                        .build()
        );

        // When
        transactionService.initiateTransfer(request);

        // Then - idempotency key is stored with the transaction (verified in the stub above)
        verify(transactionPersistencePort, atLeastOnce()).save(any(Transaction.class));
    }
}
