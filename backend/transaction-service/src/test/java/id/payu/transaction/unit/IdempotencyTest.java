package id.payu.transaction.unit;

import id.payu.transaction.application.cqrs.command.InitiateTransferCommand;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandHandler;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandResult;
import id.payu.transaction.application.service.AuthorizationService;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.out.*;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.dto.ReserveBalanceResponse;
import org.junit.jupiter.api.BeforeEach;
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
    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private InitiateTransferCommandHandler handler;

    private String userId;
    private UUID senderAccountId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        senderAccountId = UUID.fromString(userId); // Simplified for extractAccountIdFromUserId logic
    }

    private InitiateTransferCommand createCommand(String idempotencyKey) {
        return new InitiateTransferCommand(
                senderAccountId,
                "1234567890",
                Money.idr(new BigDecimal("50000")),
                "Transaction description",
                InitiateTransferRequest.TransactionType.INTERNAL_TRANSFER,
                "123456",
                "device-123",
                idempotencyKey,
                userId
        );
    }

    private InitiateTransferCommand createBifastCommand(String idempotencyKey) {
        return new InitiateTransferCommand(
                senderAccountId,
                "1234567890",
                Money.idr(new BigDecimal("50000")),
                "BI-FAST description",
                InitiateTransferRequest.TransactionType.BIFAST_TRANSFER,
                "123456",
                "device-123",
                idempotencyKey,
                userId
        );
    }

    @Test
    @DisplayName("should return existing transaction when idempotency key exists")
    void shouldReturnExistingTransaction_WhenIdempotencyKeyExists() {
        // Given
        String idempotencyKey = "key-123";
        InitiateTransferCommand command = createCommand(idempotencyKey);

        Transaction existingTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .referenceNumber("TXN-EXISTING")
                .idempotencyKey(idempotencyKey)
                .status(Transaction.TransactionStatus.PENDING)
                .type(Transaction.TransactionType.INTERNAL_TRANSFER)
                .amount(Money.idr(new BigDecimal("50000")))
                .build();

        when(transactionPersistencePort.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingTransaction));

        // When
        InitiateTransferCommandResult response = handler.handle(command);

        // Then
        assertThat(response.referenceNumber()).isEqualTo("TXN-EXISTING");
        verify(walletServicePort, never()).reserveBalance(any(), any(), any());
        verify(transactionPersistencePort, never()).save(any());
    }

    @Test
    @DisplayName("should create new transaction when idempotency key does not exist")
    void shouldCreateNewTransaction_WhenIdempotencyKeyDoesNotExist() {
        // Given
        String idempotencyKey = "key-new-123";
        InitiateTransferCommand command = createCommand(idempotencyKey);

        when(transactionPersistencePort.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(transactionPersistencePort.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);
        when(walletServicePort.reserveBalance(any(), any(), any())).thenReturn(
                ReserveBalanceResponse.builder()
                        .reservationId("res-123")
                        .status("RESERVED")
                        .build()
        );

        // When
        InitiateTransferCommandResult response = handler.handle(command);

        // Then
        assertThat(response).isNotNull();
        verify(walletServicePort, times(1)).reserveBalance(any(), any(), any());
        verify(transactionPersistencePort, times(3)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("should return failed transaction when idempotency key exists for failed transaction")
    void shouldReturnFailedTransaction_WhenIdempotencyKeyExistsForFailedTransaction() {
        // Given
        String idempotencyKey = "key-failed-123";
        InitiateTransferCommand command = createBifastCommand(idempotencyKey);

        Transaction failedTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .referenceNumber("TXN-FAILED")
                .idempotencyKey(idempotencyKey)
                .status(Transaction.TransactionStatus.FAILED)
                .type(Transaction.TransactionType.BIFAST_TRANSFER)
                .amount(Money.idr(new BigDecimal("50000")))
                .failureReason("BI-FAST Timeout")
                .build();

        when(transactionPersistencePort.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(failedTransaction));

        // When
        InitiateTransferCommandResult response = handler.handle(command);

        // Then
        assertThat(response.referenceNumber()).isEqualTo("TXN-FAILED");
        assertThat(response.status()).isEqualTo("FAILED");
        verify(walletServicePort, never()).reserveBalance(any(), any(), any());
    }

    @Test
    @DisplayName("should allow retry for different idempotency key with same parameters")
    void shouldAllowRetry_WithDifferentIdempotencyKey() {
        // Given
        String firstKey = "key-first-123";
        String secondKey = "key-second-456";

        InitiateTransferCommand firstCommand = createCommand(firstKey);
        InitiateTransferCommand secondCommand = createCommand(secondKey);

        when(transactionPersistencePort.findByIdempotencyKey(anyString()))
                .thenReturn(Optional.empty());
        when(transactionPersistencePort.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);
        when(walletServicePort.reserveBalance(any(), any(), any())).thenReturn(
                ReserveBalanceResponse.builder()
                        .reservationId("res-123")
                        .status("RESERVED")
                        .build()
        );

        // When
        handler.handle(firstCommand);
        handler.handle(secondCommand);

        // Then
        verify(transactionPersistencePort, times(6)).save(any(Transaction.class));
        verify(walletServicePort, times(2)).reserveBalance(any(), any(), any());
    }

    @Test
    @DisplayName("should handle idempotency key for BI-FAST transfer")
    void shouldHandleIdempotencyKey_ForBifastTransfer() {
        // Given
        String idempotencyKey = "key-bifast-123";
        InitiateTransferCommand command = createBifastCommand(idempotencyKey);

        Transaction existingBifastTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .referenceNumber("TXN-BIFAST-EXISTING")
                .idempotencyKey(idempotencyKey)
                .status(Transaction.TransactionStatus.PENDING)
                .type(Transaction.TransactionType.BIFAST_TRANSFER)
                .amount(Money.idr(new BigDecimal("50000")))
                .description("BI-FAST Transfer")
                .metadata("{\"externalTransactionId\":\"BIFAST-EXT-123\"}")
                .build();

        when(transactionPersistencePort.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingBifastTransaction));

        // When
        InitiateTransferCommandResult response = handler.handle(command);

        // Then
        assertThat(response.referenceNumber()).isEqualTo("TXN-BIFAST-EXISTING");
        assertThat(response.status()).isEqualTo("PENDING");
        verify(walletServicePort, never()).reserveBalance(any(), any(), any());
    }

    @Test
    @DisplayName("should handle null idempotency key by always creating new transaction")
    void shouldCreateNewTransaction_WhenIdempotencyKeyIsNull() {
        // Given
        InitiateTransferCommand command = createCommand(null);

        when(transactionPersistencePort.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);
        when(walletServicePort.reserveBalance(any(), any(), any())).thenReturn(
                ReserveBalanceResponse.builder()
                        .reservationId("res-123")
                        .status("RESERVED")
                        .build()
        );

        // When
        InitiateTransferCommandResult response = handler.handle(command);

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
        InitiateTransferCommand command = createCommand(idempotencyKey);

        Transaction completedTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .referenceNumber("TXN-COMPLETED")
                .idempotencyKey(idempotencyKey)
                .status(Transaction.TransactionStatus.COMPLETED)
                .type(Transaction.TransactionType.INTERNAL_TRANSFER)
                .amount(Money.idr(new BigDecimal("50000")))
                .build();

        when(transactionPersistencePort.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(completedTransaction));

        // When
        InitiateTransferCommandResult response = handler.handle(command);

        // Then
        assertThat(response.referenceNumber()).isEqualTo("TXN-COMPLETED");
        assertThat(response.status()).isEqualTo("COMPLETED");
        verify(walletServicePort, never()).reserveBalance(any(), any(), any());
        verify(transactionPersistencePort, never()).save(any());
    }

    @Test
    @DisplayName("should store idempotency key with new transaction")
    void shouldStoreIdempotencyKey_WithNewTransaction() {
        // Given
        String idempotencyKey = "key-store-123";
        InitiateTransferCommand command = createCommand(idempotencyKey);

        when(transactionPersistencePort.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(transactionPersistencePort.save(any(Transaction.class))).thenAnswer(i -> {
            Transaction t = (Transaction) i.getArguments()[0];
            assertThat(t.getIdempotencyKey()).isEqualTo(idempotencyKey);
            return t;
        });
        when(walletServicePort.reserveBalance(any(), any(), any())).thenReturn(
                ReserveBalanceResponse.builder()
                        .reservationId("res-123")
                        .status("RESERVED")
                        .build()
        );

        // When
        handler.handle(command);

        // Then - idempotency key is stored with the transaction (verified in the stub above)
        verify(transactionPersistencePort, atLeastOnce()).save(any(Transaction.class));
    }
}
