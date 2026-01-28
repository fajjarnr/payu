package id.payu.transaction.unit;

import id.payu.transaction.application.service.TransactionService;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.out.*;
import id.payu.transaction.dto.BifastTransferResponse;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.dto.InitiateTransferResponse;
import id.payu.transaction.dto.ReserveBalanceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BiFastTransferTest {

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
    @DisplayName("should call BI-FAST service and return pending status on success")
    void shouldCallBifastService_WhenTypeIsBiFast() throws TimeoutException {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("123456")
                .amount(new BigDecimal("50000"))
                .description("BI-FAST Transfer")
                .type(InitiateTransferRequest.TransactionType.BIFAST_TRANSFER)
                .build();

        when(transactionPersistencePort.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);

        when(walletServicePort.reserveBalance(any(), any(), any())).thenReturn(
                ReserveBalanceResponse.builder().status("RESERVED").build()
        );

        when(bifastServicePort.initiateTransfer(any())).thenReturn(
                BifastTransferResponse.builder()
                        .transactionId("ext-123")
                        .status("PENDING")
                        .build()
        );

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(request);

        // Then
        assertThat(response.getStatus()).isEqualTo("PENDING");
        verify(bifastServicePort, times(1)).initiateTransfer(any());
        verify(eventPublisherPort).publishTransactionInitiated(any());
    }

    @Test
    @DisplayName("should mark transaction as failed when BI-FAST times out")
    void shouldMarkFailed_WhenBifastTimesOut() throws TimeoutException {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("123456")
                .amount(new BigDecimal("50000"))
                .description("BI-FAST Timeout")
                .type(InitiateTransferRequest.TransactionType.BIFAST_TRANSFER)
                .build();

        when(transactionPersistencePort.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);

        when(walletServicePort.reserveBalance(any(), any(), any())).thenReturn(
                ReserveBalanceResponse.builder().status("RESERVED").build()
        );

        when(bifastServicePort.initiateTransfer(any())).thenThrow(new TimeoutException("BI-FAST Timeout"));

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(request);

        // Then
        assertThat(response.getStatus()).isEqualTo("FAILED");
        verify(eventPublisherPort).publishTransactionFailed(any(), anyString());
    }

    @Test
    @DisplayName("should handle BI-FAST timeout with specific failure reason")
    void shouldHandleBifastTimeout_WithSpecificFailureReason() throws TimeoutException {
        // Given
        String timeoutMessage = "Connection timed out after 30s";
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("123456")
                .amount(new BigDecimal("50000"))
                .description("BI-FAST Connection Timeout")
                .type(InitiateTransferRequest.TransactionType.BIFAST_TRANSFER)
                .build();

        when(transactionPersistencePort.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);
        when(walletServicePort.reserveBalance(any(), any(), any())).thenReturn(
                ReserveBalanceResponse.builder().status("RESERVED").build()
        );

        when(bifastServicePort.initiateTransfer(any()))
                .thenThrow(new TimeoutException(timeoutMessage));

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(request);

        // Then
        assertThat(response.getStatus()).isEqualTo("FAILED");
        verify(eventPublisherPort).publishTransactionFailed(any(), eq("BI-FAST Timeout"));
    }

    @Test
    @DisplayName("should handle BI-FAST timeout with large amount")
    void shouldHandleBifastTimeout_WithLargeAmount() throws TimeoutException {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("123456")
                .amount(new BigDecimal("250000000")) // 250 million IDR - high value
                .description("High Value BI-FAST")
                .type(InitiateTransferRequest.TransactionType.BIFAST_TRANSFER)
                .build();

        when(transactionPersistencePort.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);
        when(walletServicePort.reserveBalance(any(), any(), any())).thenReturn(
                ReserveBalanceResponse.builder().status("RESERVED").build()
        );

        when(bifastServicePort.initiateTransfer(any()))
                .thenThrow(new TimeoutException("BI-FAST Timeout for high value transaction"));

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(request);

        // Then
        assertThat(response.getStatus()).isEqualTo("FAILED");
        verify(eventPublisherPort).publishTransactionFailed(any(), anyString());
    }

    @Test
    @DisplayName("should handle BI-FAST service exception gracefully")
    void shouldHandleException_Gracefully() throws TimeoutException {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("123456")
                .amount(new BigDecimal("50000"))
                .description("BI-FAST Service Error")
                .type(InitiateTransferRequest.TransactionType.BIFAST_TRANSFER)
                .build();

        when(transactionPersistencePort.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);
        when(walletServicePort.reserveBalance(any(), any(), any())).thenReturn(
                ReserveBalanceResponse.builder().status("RESERVED").build()
        );

        when(bifastServicePort.initiateTransfer(any()))
                .thenThrow(new RuntimeException("BI-FAST service unavailable"));

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(request);

        // Then
        assertThat(response.getStatus()).isEqualTo("FAILED");
        verify(eventPublisherPort).publishTransactionFailed(any(), contains("unavailable"));
    }

    @Test
    @DisplayName("should handle successful BI-FAST transfer with completed status")
    void shouldHandleCompletedStatus_FromBifast() throws TimeoutException {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("123456")
                .amount(new BigDecimal("50000"))
                .description("Instant BI-FAST Transfer")
                .type(InitiateTransferRequest.TransactionType.BIFAST_TRANSFER)
                .build();

        when(transactionPersistencePort.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);
        when(walletServicePort.reserveBalance(any(), any(), any())).thenReturn(
                ReserveBalanceResponse.builder().status("RESERVED").build()
        );

        // Simulated completed response
        when(bifastServicePort.initiateTransfer(any())).thenReturn(
                BifastTransferResponse.builder()
                        .transactionId("ext-completed-123")
                        .status("COMPLETED")
                        .build()
        );

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(request);

        // Then - Service should still process as completed
        assertThat(response).isNotNull();
        verify(bifastServicePort).initiateTransfer(any());
        verify(eventPublisherPort).publishTransactionInitiated(any());
    }

    @Test
    @DisplayName("should reserve balance before calling BI-FAST service")
    void shouldReserveBalance_BeforeCallingBifast() throws TimeoutException {
        // Given
        UUID accountId = UUID.randomUUID();
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(accountId)
                .recipientAccountNumber("123456")
                .amount(new BigDecimal("100000"))
                .description("BI-FAST with balance check")
                .type(InitiateTransferRequest.TransactionType.BIFAST_TRANSFER)
                .build();

        when(transactionPersistencePort.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);
        when(walletServicePort.reserveBalance(any(), any(), any())).thenReturn(
                ReserveBalanceResponse.builder()
                        .reservationId("res-123")
                        .status("RESERVED")
                        .build()
        );
        when(bifastServicePort.initiateTransfer(any())).thenReturn(
                BifastTransferResponse.builder()
                        .transactionId("ext-123")
                        .status("PENDING")
                        .build()
        );

        // When
        transactionService.initiateTransfer(request);

        // Then - Balance should be reserved before BI-FAST call
        var inOrder = inOrder(walletServicePort, bifastServicePort);
        inOrder.verify(walletServicePort).reserveBalance(eq(accountId), any(), eq(new BigDecimal("100000")));
        inOrder.verify(bifastServicePort).initiateTransfer(any());
    }
}
