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
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SKN/RTGS Transfer Tests")
class SknRgsTransferServiceTest {

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

    private UUID userId;
    private InitiateTransferRequest sknTransferRequest;
    private InitiateTransferRequest rgsTransferRequest;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        UUID senderAccountId = UUID.randomUUID();

        sknTransferRequest = new InitiateTransferRequest();
        sknTransferRequest.setSenderAccountId(senderAccountId);
        sknTransferRequest.setRecipientAccountNumber("1234567890");
        sknTransferRequest.setAmount(new BigDecimal("100000"));
        sknTransferRequest.setCurrency("IDR");
        sknTransferRequest.setDescription("SKN Transfer Test");
        sknTransferRequest.setType(InitiateTransferRequest.TransactionType.SKN_TRANSFER);

        rgsTransferRequest = new InitiateTransferRequest();
        rgsTransferRequest.setSenderAccountId(senderAccountId);
        rgsTransferRequest.setRecipientAccountNumber("0987654321");
        rgsTransferRequest.setAmount(new BigDecimal("50000000"));
        rgsTransferRequest.setCurrency("IDR");
        rgsTransferRequest.setDescription("RTGS Transfer Test");
        rgsTransferRequest.setType(InitiateTransferRequest.TransactionType.RTGS_TRANSFER);
    }

    @Test
    @DisplayName("should initiate SKN transfer successfully")
    void shouldInitiateSknTransferSuccessfully() {
        // Given
        given(transactionPersistencePort.save(any(Transaction.class))).willAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });

        given(walletServicePort.reserveBalance(any(), anyString(), any())).willReturn(
                ReserveBalanceResponse.builder()
                        .reservationId("res-skn-123")
                        .status("RESERVED")
                        .build()
        );

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(sknTransferRequest, userId.toString());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("VALIDATING");
        assertThat(response.getFee()).isEqualTo(new BigDecimal("5000"));
        assertThat(response.getEstimatedCompletionTime()).isEqualTo("Same day");
        verify(transactionPersistencePort, times(2)).save(any(Transaction.class));
        verify(eventPublisherPort).publishTransactionInitiated(any(Transaction.class));
        verify(walletServicePort).reserveBalance(eq(sknTransferRequest.getSenderAccountId()), anyString(), eq(sknTransferRequest.getAmount()));
    }

    @Test
    @DisplayName("should fail SKN transfer when balance insufficient")
    void shouldFailSknTransferWhenBalanceInsufficient() {
        // Given
        given(transactionPersistencePort.save(any(Transaction.class))).willAnswer(invocation -> invocation.getArgument(0));

        given(walletServicePort.reserveBalance(any(), anyString(), any())).willReturn(
                ReserveBalanceResponse.builder()
                        .status("FAILED")
                        .build()
        );

        // When/Then
        assertThatThrownBy(() -> transactionService.initiateTransfer(sknTransferRequest, userId.toString()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Insufficient balance");

        verify(eventPublisherPort).publishTransactionFailed(any(Transaction.class), anyString());
    }

    @Test
    @DisplayName("should initiate RTGS transfer successfully")
    void shouldInitiateRgsTransferSuccessfully() {
        // Given
        given(transactionPersistencePort.save(any(Transaction.class))).willAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });

        given(walletServicePort.reserveBalance(any(), anyString(), any())).willReturn(
                ReserveBalanceResponse.builder()
                        .reservationId("res-rgs-456")
                        .status("RESERVED")
                        .build()
        );

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(rgsTransferRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("VALIDATING");
        assertThat(response.getFee()).isEqualTo(new BigDecimal("25000"));
        assertThat(response.getEstimatedCompletionTime()).isEqualTo("Real-time");
        verify(transactionPersistencePort, times(2)).save(any(Transaction.class));
        verify(eventPublisherPort).publishTransactionInitiated(any(Transaction.class));
        verify(walletServicePort).reserveBalance(eq(rgsTransferRequest.getSenderAccountId()), anyString(), eq(rgsTransferRequest.getAmount()));
    }

    @Test
    @DisplayName("should fail RTGS transfer when balance insufficient")
    void shouldFailRgsTransferWhenBalanceInsufficient() {
        // Given
        given(transactionPersistencePort.save(any(Transaction.class))).willAnswer(invocation -> invocation.getArgument(0));

        given(walletServicePort.reserveBalance(any(), anyString(), any())).willReturn(
                ReserveBalanceResponse.builder()
                        .status("FAILED")
                        .build()
        );

        // When/Then
        assertThatThrownBy(() -> transactionService.initiateTransfer(rgsTransferRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Insufficient balance");

        verify(eventPublisherPort).publishTransactionFailed(any(Transaction.class), anyString());
    }

    @Test
    @DisplayName("should calculate correct SKN fee")
    void shouldCalculateCorrectSknFee() {
        // Given
        given(transactionPersistencePort.save(any(Transaction.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(walletServicePort.reserveBalance(any(), anyString(), any())).willReturn(
                ReserveBalanceResponse.builder().status("RESERVED").build()
        );

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(sknTransferRequest, userId.toString());

        // Then
        assertThat(response.getFee()).isEqualTo(new BigDecimal("5000"));
    }

    @Test
    @DisplayName("should calculate correct RTGS fee")
    void shouldCalculateCorrectRgsFee() {
        // Given
        given(transactionPersistencePort.save(any(Transaction.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(walletServicePort.reserveBalance(any(), anyString(), any())).willReturn(
                ReserveBalanceResponse.builder().status("RESERVED").build()
        );

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(rgsTransferRequest);

        // Then
        assertThat(response.getFee()).isEqualTo(new BigDecimal("25000"));
    }

    @Test
    @DisplayName("should set correct estimated completion time for SKN")
    void shouldSetCorrectEstimatedCompletionTimeForSkn() {
        // Given
        given(transactionPersistencePort.save(any(Transaction.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(walletServicePort.reserveBalance(any(), anyString(), any())).willReturn(
                ReserveBalanceResponse.builder().status("RESERVED").build()
        );

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(sknTransferRequest, userId.toString());

        // Then
        assertThat(response.getEstimatedCompletionTime()).isEqualTo("Same day");
    }

    @Test
    @DisplayName("should set correct estimated completion time for RTGS")
    void shouldSetCorrectEstimatedCompletionTimeForRgs() {
        // Given
        given(transactionPersistencePort.save(any(Transaction.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(walletServicePort.reserveBalance(any(), anyString(), any())).willReturn(
                ReserveBalanceResponse.builder().status("RESERVED").build()
        );

        // When
        InitiateTransferResponse response = transactionService.initiateTransfer(rgsTransferRequest);

        // Then
        assertThat(response.getEstimatedCompletionTime()).isEqualTo("Real-time");
    }
}
