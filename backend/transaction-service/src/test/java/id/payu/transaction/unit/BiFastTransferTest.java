package id.payu.transaction.unit;

import id.payu.transaction.application.cqrs.command.InitiateTransferCommand;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandHandler;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandResult;
import id.payu.transaction.application.service.AuthorizationService;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.out.*;
import id.payu.transaction.dto.BifastTransferResponse;
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

    private InitiateTransferCommand createBifastCommand() {
        return new InitiateTransferCommand(
                senderAccountId,
                "1234567890",
                Money.idr(new BigDecimal("50000")),
                "BI-FAST Transfer",
                InitiateTransferRequest.TransactionType.BIFAST_TRANSFER,
                "123456",
                "device-123",
                null,
                userId
        );
    }

    @Test
    @DisplayName("should call BI-FAST service and return pending status on success")
    void shouldCallBifastService_WhenTypeIsBiFast() throws Exception {
        // Given
        InitiateTransferCommand command = createBifastCommand();

        when(transactionPersistencePort.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);
        when(walletServicePort.reserveBalance(any(), any(), any())).thenReturn(
                ReserveBalanceResponse.builder().status("RESERVED").build()
        );

        // When
        InitiateTransferCommandResult response = handler.handle(command);

        // Then
        assertThat(response.status()).isEqualTo("PENDING");
        verify(bifastServicePort, times(1)).initiateTransfer(any());
        verify(eventPublisherPort).publishTransactionInitiated(any());
    }

    @Test
    @DisplayName("should mark transaction as failed when BI-FAST service throws exception")
    void shouldMarkFailed_WhenBifastThrows() throws Exception {
        // Given
        InitiateTransferCommand command = createBifastCommand();

        when(transactionPersistencePort.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);
        when(walletServicePort.reserveBalance(any(), any(), any())).thenReturn(
                ReserveBalanceResponse.builder().status("RESERVED").build()
        );
        doThrow(new RuntimeException("BI-FAST Timeout")).when(bifastServicePort).initiateTransfer(any());

        // When
        InitiateTransferCommandResult response = handler.handle(command);

        // Then
        assertThat(response.status()).isEqualTo("FAILED");
        verify(eventPublisherPort).publishTransactionFailed(any(), anyString());
    }

    @Test
    @DisplayName("should handle successful BI-FAST transfer and call save correctly")
    void shouldHandleBifastSuccess() throws Exception {
        // Given
        InitiateTransferCommand command = createBifastCommand();

        when(transactionPersistencePort.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);
        when(walletServicePort.reserveBalance(any(), any(), any())).thenReturn(
                ReserveBalanceResponse.builder().status("RESERVED").build()
        );

        // When
        InitiateTransferCommandResult response = handler.handle(command);

        // Then
        assertThat(response).isNotNull();
        verify(bifastServicePort).initiateTransfer(any());
        verify(transactionPersistencePort, atLeastOnce()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("should reserve balance before calling BI-FAST service")
    void shouldReserveBalance_BeforeCallingBifast() throws Exception {
        // Given
        InitiateTransferCommand command = createBifastCommand();

        when(transactionPersistencePort.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);
        when(walletServicePort.reserveBalance(any(), any(), any())).thenReturn(
                ReserveBalanceResponse.builder()
                        .reservationId("res-123")
                        .status("RESERVED")
                        .build()
        );

        // When
        handler.handle(command);

        // Then - Balance should be reserved before BI-FAST call
        var inOrder = inOrder(walletServicePort, bifastServicePort);
        inOrder.verify(walletServicePort).reserveBalance(eq(senderAccountId), any(), eq(new BigDecimal("50000.00")));
        inOrder.verify(bifastServicePort).initiateTransfer(any());
    }
}
