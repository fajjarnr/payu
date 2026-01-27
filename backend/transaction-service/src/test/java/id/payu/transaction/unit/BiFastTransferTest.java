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
}
