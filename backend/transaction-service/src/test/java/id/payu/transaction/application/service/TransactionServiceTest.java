package id.payu.transaction.application.service;

import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.out.*;
import id.payu.transaction.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionPersistencePort transactionPersistencePort;
    @Mock
    private WalletServicePort walletServicePort;
    @Mock
    private BifastServicePort bifastServicePort;
    @Mock
    private QrisServicePort qrisServicePort;
    @Mock
    private TransactionEventPublisherPort eventPublisherPort;

    @InjectMocks
    private TransactionService transactionService;

    private InitiateTransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        transferRequest = new InitiateTransferRequest();
        transferRequest.setSenderAccountId(UUID.randomUUID());
        transferRequest.setRecipientAccountNumber("1234567890");
        transferRequest.setAmount(new BigDecimal("100000"));
        transferRequest.setCurrency("IDR");
        transferRequest.setDescription("Test Transfer");
        transferRequest.setType(InitiateTransferRequest.TransactionType.INTERNAL_TRANSFER);
    }

    @Test
    @DisplayName("should initiate transfer successfully")
    void shouldInitiateTransferSuccessfully() {
        // Given
        given(transactionPersistencePort.save(any(Transaction.class))).willAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });

        given(walletServicePort.reserveBalance(any(), anyString(), any())).willReturn(
                ReserveBalanceResponse.builder().success(true).build()
        );

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(transferRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("PENDING");
        verify(transactionPersistencePort, times(2)).save(any(Transaction.class)); // Saved initially and after validation update
        verify(eventPublisherPort).publishTransactionInitiated(any(Transaction.class));
        verify(walletServicePort).reserveBalance(eq(transferRequest.getSenderAccountId()), anyString(), eq(transferRequest.getAmount()));
    }

    @Test
    @DisplayName("should fail transfer when balance insufficient")
    void shouldFailTransferWhenBalanceInsufficient() {
        // Given
        given(transactionPersistencePort.save(any(Transaction.class))).willAnswer(invocation -> invocation.getArgument(0));

        given(walletServicePort.reserveBalance(any(), anyString(), any())).willReturn(
                ReserveBalanceResponse.builder().success(false).message("Insufficient balance").build()
        );

        // When/Then
        assertThatThrownBy(() -> transactionService.initiateTransfer(transferRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Insufficient balance");

        verify(eventPublisherPort).publishTransactionFailed(any(Transaction.class), anyString());
    }

    @Test
    @DisplayName("should process QRIS payment successfully")
    void shouldProcessQrisPaymentSuccessfully() {
        // Given
        ProcessQrisPaymentRequest request = new ProcessQrisPaymentRequest();
        request.setQrisCode("ValidQRISCode");
        request.setAmount(new BigDecimal("50000"));

        given(transactionPersistencePort.save(any(Transaction.class))).willAnswer(invocation -> invocation.getArgument(0));
        
        given(qrisServicePort.processPayment(any(QrisPaymentRequest.class))).willReturn(
                QrisPaymentResponse.builder().status("SUCCESS").build()
        );

        // When
        transactionService.processQrisPayment(request);

        // Then
        verify(eventPublisherPort).publishTransactionCompleted(any(Transaction.class));
    }
}
