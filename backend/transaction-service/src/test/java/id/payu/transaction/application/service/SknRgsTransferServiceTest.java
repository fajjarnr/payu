package id.payu.transaction.application.service;

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
        senderAccountId = UUID.fromString(userId);
    }

    private InitiateTransferCommand createSknCommand() {
        return new InitiateTransferCommand(
                senderAccountId,
                "1234567890",
                Money.idr(new BigDecimal("100000")),
                "SKN Transfer Test",
                InitiateTransferRequest.TransactionType.SKN_TRANSFER,
                "123456",
                "device-123",
                null,
                userId
        );
    }

    private InitiateTransferCommand createRgsCommand() {
        return new InitiateTransferCommand(
                senderAccountId,
                "0987654321",
                Money.idr(new BigDecimal("50000000")),
                "RTGS Transfer Test",
                InitiateTransferRequest.TransactionType.RTGS_TRANSFER,
                "123456",
                "device-123",
                null,
                userId
        );
    }

    @Test
    @DisplayName("should initiate SKN transfer successfully")
    void shouldInitiateSknTransferSuccessfully() {
        // Given
        InitiateTransferCommand command = createSknCommand();
        given(transactionPersistencePort.save(any(Transaction.class))).willAnswer(invocation -> invocation.getArgument(0));

        given(walletServicePort.reserveBalance(any(), anyString(), any())).willReturn(
                ReserveBalanceResponse.builder()
                        .reservationId("res-skn-123")
                        .status("RESERVED")
                        .build()
        );

        // When
        InitiateTransferCommandResult result = handler.handle(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.fee()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(result.estimatedCompletionTime()).isEqualTo("Same day");
        verify(transactionPersistencePort, times(3)).save(any(Transaction.class));
        verify(walletServicePort).reserveBalance(eq(senderAccountId), anyString(), eq(new BigDecimal("100000.00")));
    }

    @Test
    @DisplayName("should initiate RTGS transfer successfully")
    void shouldInitiateRgsTransferSuccessfully() {
        // Given
        InitiateTransferCommand command = createRgsCommand();
        given(transactionPersistencePort.save(any(Transaction.class))).willAnswer(invocation -> invocation.getArgument(0));

        given(walletServicePort.reserveBalance(any(), anyString(), any())).willReturn(
                ReserveBalanceResponse.builder()
                        .reservationId("res-rgs-456")
                        .status("RESERVED")
                        .build()
        );

        // When
        InitiateTransferCommandResult result = handler.handle(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.fee()).isEqualByComparingTo(new BigDecimal("25000"));
        assertThat(result.estimatedCompletionTime()).isEqualTo("Real-time");
        verify(transactionPersistencePort, times(3)).save(any(Transaction.class));
        verify(walletServicePort).reserveBalance(eq(senderAccountId), anyString(), eq(new BigDecimal("50000000.00")));
    }

    @Test
    @DisplayName("should fail transfer when balance insufficient")
    void shouldFailTransferWhenBalanceInsufficient() {
        // Given
        InitiateTransferCommand command = createSknCommand();
        given(transactionPersistencePort.save(any(Transaction.class))).willAnswer(invocation -> invocation.getArgument(0));

        given(walletServicePort.reserveBalance(any(), anyString(), any())).willReturn(
                ReserveBalanceResponse.builder()
                        .status("FAILED")
                        .build()
        );

        // When/Then
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Insufficient balance");

        verify(eventPublisherPort).publishTransactionFailed(any(Transaction.class), anyString());
    }
}
