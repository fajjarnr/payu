package id.payu.transaction.saga;

import id.payu.transaction.application.cqrs.command.InitiateTransferCommand;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandHandler;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandResult;
import id.payu.transaction.application.service.AuthorizationService;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.out.*;
import id.payu.transaction.dto.BifastTransferRequest;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.dto.ReserveBalanceResponse;
import id.payu.transaction.exception.TransactionDomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Saga Compensation Tests for Transaction Service.
 *
 * <p>These tests verify the saga pattern implementation for distributed transaction
 * compensation. The saga pattern ensures that when a multi-step transaction fails,
 * all previously completed steps are compensated (rolled back).</p>
 *
 * <p><b>Transaction Flow:</b></p>
 * <ol>
 *   <li>Create transaction (PENDING status)</li>
 *   <li>Reserve balance from wallet service</li>
 *   <li>Initiate BiFast transfer</li>
 *   <li>Update transaction status to COMPLETED</li>
 * </ol>
 *
 * <p><b>Compensation Flow (on failure):</b></p>
 * <ol>
 *   <li>Release reserved balance back to wallet</li>
 *   <li>Update transaction status to FAILED</li>
 *   <li>Publish transaction failed event</li>
 * </ol>
 *
 * <p><b>Test Categories:</b></p>
 * <ul>
 *   <li>Happy Path: All steps complete successfully</li>
 *   <li>Balance Reservation Failure: Compensate immediately</li>
 *   <li>BiFast Transfer Failure: Release balance, mark failed</li>
 *   <li>Timeout Scenarios: Automatic compensation</li>
 * </ul>
 *
 * @see InitiateTransferCommandHandler
 * @see WalletServicePort
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Saga Compensation Tests")
class SagaCompensationTest {

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
    private TransactionEventPublisherPort eventPublisherPort;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private InitiateTransferCommandHandler commandHandler;

    private UUID senderAccountId;
    private UUID userId;
    private Money transferAmount;
    private String recipientAccountNumber;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        senderAccountId = UUID.randomUUID();
        userId = UUID.randomUUID();
        transferAmount = Money.idr("100000");
        recipientAccountNumber = "1234567890";
        idempotencyKey = UUID.randomUUID().toString();
    }

    // ==================== HAPPY PATH TESTS ====================

    @Nested
    @DisplayName("Happy Path - Successful Transaction")
    class HappyPathTests {

        @Test
        @DisplayName("Should complete transfer when all steps succeed")
        void shouldCompleteTransferWhenAllStepsSucceed() {
            // Given
            InitiateTransferCommand command = createTransferCommand();

            Transaction savedTransaction = Transaction.builder()
                    .id(UUID.randomUUID())
                    .referenceNumber("TXN123456")
                    .status(Transaction.TransactionStatus.PENDING)
                    .build();

            when(transactionPersistencePort.save(any(Transaction.class)))
                    .thenReturn(savedTransaction);
            when(walletServicePort.reserveBalance(any(), any(), any()))
                    .thenReturn(ReserveBalanceResponse.builder()
                            .reservationId("res-123")
                            .status("RESERVED")
                            .build());

            // When
            InitiateTransferCommandResult result = commandHandler.handle(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.transactionId()).isNotNull();

            // Verify transaction was created
            verify(transactionPersistencePort, atLeastOnce()).save(any(Transaction.class));

            // Verify balance was reserved
            verify(walletServicePort).reserveBalance(
                    eq(senderAccountId),
                    anyString(),
                    eq(transferAmount.getAmount())
            );

            // Verify event was published
            verify(eventPublisherPort).publishTransactionInitiated(any(Transaction.class));
        }

        @Test
        @DisplayName("Should not compensate when BiFast transfer succeeds")
        void shouldNotCompensateWhenBiFastTransferSucceeds() {
            // Given
            InitiateTransferCommand command = createTransferCommand();

            Transaction savedTransaction = Transaction.builder()
                    .id(UUID.randomUUID())
                    .referenceNumber("TXN123456")
                    .status(Transaction.TransactionStatus.PENDING)
                    .build();

            when(transactionPersistencePort.save(any(Transaction.class)))
                    .thenReturn(savedTransaction);
            when(walletServicePort.reserveBalance(any(), any(), any()))
                    .thenReturn(ReserveBalanceResponse.builder()
                            .reservationId("res-123")
                            .status("RESERVED")
                            .build());

            // When
            commandHandler.handle(command);

            // Then - Balance release should NOT be called
            verify(walletServicePort, never()).releaseBalance(any(), any(), any());
        }
    }

    // ==================== COMPENSATION TESTS ====================

    @Nested
    @DisplayName("Balance Reservation Failure - Immediate Compensation")
    class BalanceReservationFailureTests {

        @Test
        @DisplayName("Should fail gracefully when balance reservation fails")
        void shouldFailGracefullyWhenBalanceReservationFails() {
            // Given
            InitiateTransferCommand command = createTransferCommand();

            Transaction savedTransaction = Transaction.builder()
                    .id(UUID.randomUUID())
                    .referenceNumber("TXN123456")
                    .status(Transaction.TransactionStatus.PENDING)
                    .build();

            when(transactionPersistencePort.save(any(Transaction.class)))
                    .thenReturn(savedTransaction);
            when(walletServicePort.reserveBalance(any(), any(), any()))
                    .thenReturn(ReserveBalanceResponse.builder()
                            .status("FAILED")
                            .build());

            // When & Then
            assertThatThrownBy(() -> commandHandler.handle(command))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient balance");

            // Verify transaction status updated to FAILED
            ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionPersistencePort, atLeast(2)).save(transactionCaptor.capture());

            Transaction failedTransaction = transactionCaptor.getAllValues().get(1);
            assertThat(failedTransaction.getStatus()).isEqualTo(Transaction.TransactionStatus.FAILED);
            assertThat(failedTransaction.getFailureReason()).contains("Insufficient balance");

            // Verify failed event was published
            verify(eventPublisherPort).publishTransactionFailed(
                    any(Transaction.class),
                    eq("Insufficient balance")
            );
        }

        @Test
        @DisplayName("Should publish transaction failed event on insufficient balance")
        void shouldPublishTransactionFailedEventOnInsufficientBalance() {
            // Given
            InitiateTransferCommand command = createTransferCommand();

            Transaction savedTransaction = Transaction.builder()
                    .id(UUID.randomUUID())
                    .referenceNumber("TXN123456")
                    .status(Transaction.TransactionStatus.PENDING)
                    .build();

            when(transactionPersistencePort.save(any(Transaction.class)))
                    .thenReturn(savedTransaction);
            when(walletServicePort.reserveBalance(any(), any(), any()))
                    .thenReturn(ReserveBalanceResponse.builder()
                            .status("FAILED")
                            .build());

            // When
            try {
                commandHandler.handle(command);
            } catch (IllegalStateException e) {
                // Expected
            }

            // Then
            verify(eventPublisherPort).publishTransactionFailed(
                    any(Transaction.class),
                    eq("Insufficient balance")
            );
        }
    }

    @Nested
    @DisplayName("BiFast Transfer Failure - Balance Release Compensation")
    class BifastTransferFailureTests {

        @Test
        @DisplayName("Should release balance when BiFast transfer fails")
        void shouldReleaseBalanceWhenBiFastTransferFails() {
            // Given
            InitiateTransferCommand command = createTransferCommand();

            Transaction savedTransaction = Transaction.builder()
                    .id(UUID.randomUUID())
                    .referenceNumber("TXN123456")
                    .status(Transaction.TransactionStatus.PENDING)
                    .build();

            when(transactionPersistencePort.save(any(Transaction.class)))
                    .thenReturn(savedTransaction);
            when(walletServicePort.reserveBalance(any(), any(), any()))
                    .thenReturn(ReserveBalanceResponse.builder()
                            .reservationId("res-123")
                            .status("RESERVED")
                            .build());
            doThrow(new RuntimeException("BiFast service unavailable"))
                    .when(bifastServicePort).initiateTransfer(any(BifastTransferRequest.class));

            // When
            InitiateTransferCommandResult result = commandHandler.handle(command);

            // Then - Balance should be released (compensation)
            verify(walletServicePort).releaseBalance(
                    eq(senderAccountId),
                    anyString(),
                    eq(transferAmount.getAmount())
            );

            // Verify transaction status updated to FAILED
            ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionPersistencePort, atLeast(2)).save(transactionCaptor.capture());

            Transaction failedTransaction = transactionCaptor.getAllValues().get(1);
            assertThat(failedTransaction.getStatus()).isEqualTo(Transaction.TransactionStatus.FAILED);
            assertThat(failedTransaction.getFailureReason()).contains("BiFast service unavailable");
        }

        @Test
        @DisplayName("Should publish failed event when BiFast transfer fails")
        void shouldPublishFailedEventWhenBiFastTransferFails() {
            // Given
            InitiateTransferCommand command = createTransferCommand();

            Transaction savedTransaction = Transaction.builder()
                    .id(UUID.randomUUID())
                    .referenceNumber("TXN123456")
                    .status(Transaction.TransactionStatus.PENDING)
                    .build();

            when(transactionPersistencePort.save(any(Transaction.class)))
                    .thenReturn(savedTransaction);
            when(walletServicePort.reserveBalance(any(), any(), any()))
                    .thenReturn(ReserveBalanceResponse.builder()
                            .reservationId("res-123")
                            .status("RESERVED")
                            .build());
            doThrow(new RuntimeException("BiFast timeout"))
                    .when(bifastServicePort).initiateTransfer(any(BifastTransferRequest.class));

            // When
            try {
                commandHandler.handle(command);
            } catch (Exception e) {
                // Expected
            }

            // Then
            verify(eventPublisherPort).publishTransactionFailed(
                    any(Transaction.class),
                    eq("BiFast timeout")
            );
        }

        @Test
        @DisplayName("Should not commit balance when BiFast transfer fails")
        void shouldNotCommitBalanceWhenBiFastTransferFails() {
            // Given
            InitiateTransferCommand command = createTransferCommand();

            Transaction savedTransaction = Transaction.builder()
                    .id(UUID.randomUUID())
                    .referenceNumber("TXN123456")
                    .status(Transaction.TransactionStatus.PENDING)
                    .build();

            when(transactionPersistencePort.save(any(Transaction.class)))
                    .thenReturn(savedTransaction);
            when(walletServicePort.reserveBalance(any(), any(), any()))
                    .thenReturn(ReserveBalanceResponse.builder()
                            .reservationId("res-123")
                            .status("RESERVED")
                            .build());
            doThrow(new RuntimeException("BiFast failure"))
                    .when(bifastServicePort).initiateTransfer(any(BifastTransferRequest.class));

            // When
            commandHandler.handle(command);

            // Then - Commit should NOT be called, release should be called
            verify(walletServicePort, never()).commitBalance(any(), any(), any());
            verify(walletServicePort).releaseBalance(any(), any(), any());
        }
    }

    // ==================== TIMEOUT SCENARIOS ====================

    @Nested
    @DisplayName("Timeout Scenarios - Automatic Compensation")
    class TimeoutScenariosTests {

        @Test
        @DisplayName("Should handle wallet service timeout gracefully")
        void shouldHandleWalletServiceTimeoutGracefully() {
            // Given
            InitiateTransferCommand command = createTransferCommand();

            when(walletServicePort.reserveBalance(any(), any(), any()))
                    .thenThrow(new RuntimeException("Wallet service timeout"));

            // When & Then
            assertThatThrownBy(() -> commandHandler.handle(command))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Wallet service timeout");

            // Verify no balance release is attempted (nothing to release)
            verify(walletServicePort, never()).releaseBalance(any(), any(), any());
        }

        @Test
        @DisplayName("Should handle BiFast service timeout gracefully")
        void shouldHandleBiFastServiceTimeoutGracefully() {
            // Given
            InitiateTransferCommand command = createTransferCommand();

            Transaction savedTransaction = Transaction.builder()
                    .id(UUID.randomUUID())
                    .referenceNumber("TXN123456")
                    .status(Transaction.TransactionStatus.PENDING)
                    .build();

            when(transactionPersistencePort.save(any(Transaction.class)))
                    .thenReturn(savedTransaction);
            when(walletServicePort.reserveBalance(any(), any(), any()))
                    .thenReturn(ReserveBalanceResponse.builder()
                            .reservationId("res-123")
                            .status("RESERVED")
                            .build());
            when(bifastServicePort.initiateTransfer(any()))
                    .thenThrow(new RuntimeException("Read timeout"));

            // When
            commandHandler.handle(command);

            // Then - Balance should be released as compensation
            verify(walletServicePort).releaseBalance(
                    eq(senderAccountId),
                    anyString(),
                    eq(transferAmount.getAmount())
            );
        }
    }

    // ==================== CONCURRENT OPERATIONS ====================

    @Nested
    @DisplayName("Concurrent Operations - Race Condition Tests")
    class ConcurrentOperationsTests {

        @Test
        @DisplayName("Should handle duplicate idempotency keys correctly")
        void shouldHandleDuplicateIdempotencyKeysCorrectly() {
            // Given
            InitiateTransferCommand command = createTransferCommand();

            UUID existingTransactionId = UUID.randomUUID();
            InitiateTransferCommandResult existingResult = new InitiateTransferCommandResult(
                    existingTransactionId,
                    "TXN123456",
                    "COMPLETED",
                    BigDecimal.ZERO,
                    "2 seconds"
            );

            when(transactionPersistencePort.findByIdempotencyKey(idempotencyKey))
                    .thenReturn(java.util.Optional.of(Transaction.builder()
                            .id(existingTransactionId)
                            .referenceNumber("TXN123456")
                            .status(Transaction.TransactionStatus.PENDING)
                            .build()));

            // When
            InitiateTransferCommandResult result = commandHandler.handle(command);

            // Then - Should return existing result without processing
            assertThat(result.transactionId()).isEqualTo(existingTransactionId);

            // Verify balance was NOT reserved again
            verify(walletServicePort, never()).reserveBalance(any(), any(), any());
        }
    }

    // ==================== TRANSACTION STATUS TESTS ====================

    @Nested
    @DisplayName("Transaction Status Transitions")
    class TransactionStatusTransitionsTests {

        @Test
        @DisplayName("Should follow correct status transition: PENDING -> VALIDATING -> PENDING/COMPLETED")
        void shouldFollowCorrectStatusTransition() {
            // Given
            InitiateTransferCommand command = createTransferCommand();

            Transaction savedTransaction = Transaction.builder()
                    .id(UUID.randomUUID())
                    .referenceNumber("TXN123456")
                    .status(Transaction.TransactionStatus.PENDING)
                    .build();

            when(transactionPersistencePort.save(any(Transaction.class)))
                    .thenReturn(savedTransaction);
            when(walletServicePort.reserveBalance(any(), any(), any()))
                    .thenReturn(ReserveBalanceResponse.builder()
                            .reservationId("res-123")
                            .status("RESERVED")
                            .build());

            // When
            commandHandler.handle(command);

            // Then - Verify status transitions
            ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionPersistencePort, atLeast(2)).save(transactionCaptor.capture());

            // First save: PENDING
            Transaction firstTransaction = transactionCaptor.getAllValues().get(0);
            assertThat(firstTransaction.getStatus()).isEqualTo(Transaction.TransactionStatus.PENDING);

            // Second save: VALIDATING
            Transaction secondTransaction = transactionCaptor.getAllValues().get(1);
            assertThat(secondTransaction.getStatus()).isEqualTo(Transaction.TransactionStatus.VALIDATING);
        }

        @Test
        @DisplayName("Should transition to FAILED when balance reservation fails")
        void shouldTransitionToFailedWhenBalanceReservationFails() {
            // Given
            InitiateTransferCommand command = createTransferCommand();

            Transaction savedTransaction = Transaction.builder()
                    .id(UUID.randomUUID())
                    .referenceNumber("TXN123456")
                    .status(Transaction.TransactionStatus.PENDING)
                    .build();

            when(transactionPersistencePort.save(any(Transaction.class)))
                    .thenReturn(savedTransaction);
            when(walletServicePort.reserveBalance(any(), any(), any()))
                    .thenReturn(ReserveBalanceResponse.builder()
                            .status("FAILED")
                            .build());

            // When
            try {
                commandHandler.handle(command);
            } catch (IllegalStateException e) {
                // Expected
            }

            // Then
            ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionPersistencePort, atLeast(2)).save(transactionCaptor.capture());

            Transaction failedTransaction = transactionCaptor.getAllValues().get(1);
            assertThat(failedTransaction.getStatus()).isEqualTo(Transaction.TransactionStatus.FAILED);
        }
    }

    // ==================== EVENT PUBLISHING TESTS ====================

    @Nested
    @DisplayName("Event Publishing - Saga Coordination")
    class EventPublishingTests {

        @Test
        @DisplayName("Should publish transaction initiated event")
        void shouldPublishTransactionInitiatedEvent() {
            // Given
            InitiateTransferCommand command = createTransferCommand();

            Transaction savedTransaction = Transaction.builder()
                    .id(UUID.randomUUID())
                    .referenceNumber("TXN123456")
                    .status(Transaction.TransactionStatus.PENDING)
                    .build();

            when(transactionPersistencePort.save(any(Transaction.class)))
                    .thenReturn(savedTransaction);
            when(walletServicePort.reserveBalance(any(), any(), any()))
                    .thenReturn(ReserveBalanceResponse.builder()
                            .reservationId("res-123")
                            .status("RESERVED")
                            .build());

            // When
            commandHandler.handle(command);

            // Then
            verify(eventPublisherPort).publishTransactionInitiated(any(Transaction.class));
        }

        @Test
        @DisplayName("Should publish failed event with correct failure reason")
        void shouldPublishFailedEventWithCorrectFailureReason() {
            // Given
            String expectedFailureReason = "BiFast service unavailable";
            InitiateTransferCommand command = createTransferCommand();

            Transaction savedTransaction = Transaction.builder()
                    .id(UUID.randomUUID())
                    .referenceNumber("TXN123456")
                    .status(Transaction.TransactionStatus.PENDING)
                    .build();

            when(transactionPersistencePort.save(any(Transaction.class)))
                    .thenReturn(savedTransaction);
            when(walletServicePort.reserveBalance(any(), any(), any()))
                    .thenReturn(ReserveBalanceResponse.builder()
                            .reservationId("res-123")
                            .status("RESERVED")
                            .build());
            doThrow(new RuntimeException(expectedFailureReason))
                    .when(bifastServicePort).initiateTransfer(any(BifastTransferRequest.class));

            // When
            commandHandler.handle(command);

            // Then
            ArgumentCaptor<String> failureReasonCaptor = ArgumentCaptor.forClass(String.class);
            verify(eventPublisherPort).publishTransactionFailed(
                    any(Transaction.class),
                    failureReasonCaptor.capture()
            );

            assertThat(failureReasonCaptor.getValue()).isEqualTo(expectedFailureReason);
        }
    }

    // ==================== HELPER METHODS ====================

    private InitiateTransferCommand createTransferCommand() {
        return new InitiateTransferCommand(
                senderAccountId,
                recipientAccountNumber,
                transferAmount,
                "Test transfer",
                InitiateTransferRequest.TransactionType.BIFAST_TRANSFER,
                null,
                "device-123",
                null,
                userId.toString()
        );
    }
}
