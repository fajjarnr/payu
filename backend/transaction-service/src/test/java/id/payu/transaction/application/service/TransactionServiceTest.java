package id.payu.transaction.application.service;

import id.payu.transaction.application.cqrs.command.InitiateTransferCommand;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandHandler;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandResult;
import id.payu.transaction.application.cqrs.command.ProcessQrisPaymentCommand;
import id.payu.transaction.application.cqrs.command.ProcessQrisPaymentCommandHandler;
import id.payu.transaction.application.cqrs.query.GetAccountTransactionsQuery;
import id.payu.transaction.application.cqrs.query.GetAccountTransactionsQueryHandler;
import id.payu.transaction.application.cqrs.query.GetTransactionQuery;
import id.payu.transaction.application.cqrs.query.GetTransactionQueryHandler;
import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.dto.ProcessQrisPaymentRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private InitiateTransferCommandHandler initiateTransferHandler;
    @Mock
    private ProcessQrisPaymentCommandHandler processQrisPaymentHandler;
    @Mock
    private GetTransactionQueryHandler getTransactionHandler;
    @Mock
    private GetAccountTransactionsQueryHandler getAccountTransactionsQueryHandler;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("should delegate initiateTransfer to handler")
    void shouldDelegateInitiateTransferToHandler() {
        // Given
        InitiateTransferCommand command = mock(InitiateTransferCommand.class);
        InitiateTransferCommandResult expectedResult = mock(InitiateTransferCommandResult.class);
        when(initiateTransferHandler.handle(command)).thenReturn(expectedResult);

        // When
        InitiateTransferCommandResult result = transactionService.initiateTransfer(command);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        verify(initiateTransferHandler).handle(command);
    }

    @Test
    @DisplayName("should delegate processQrisPayment to handler")
    void shouldDelegateProcessQrisPaymentToHandler() {
        // Given
        ProcessQrisPaymentCommand command = mock(ProcessQrisPaymentCommand.class);

        // When
        transactionService.processQrisPayment(command);

        // Then
        verify(processQrisPaymentHandler).handle(command);
    }

    @Test
    @DisplayName("should delegate getTransaction to handler")
    void shouldDelegateGetTransactionToHandler() {
        // Given
        GetTransactionQuery query = mock(GetTransactionQuery.class);
        TransactionEntity expectedTransaction = mock(TransactionEntity.class);
        when(getTransactionHandler.handle(query)).thenReturn(expectedTransaction);

        // When
        TransactionEntity result = transactionService.getTransaction(query);

        // Then
        assertThat(result).isEqualTo(expectedTransaction);
        verify(getTransactionHandler).handle(query);
    }

    @Test
    @DisplayName("should delegate getAccountTransactions to handler")
    void shouldDelegateGetAccountTransactionsToHandler() {
        // Given
        GetAccountTransactionsQuery query = mock(GetAccountTransactionsQuery.class);
        List<TransactionEntity> expectedList = Collections.emptyList();
        when(getAccountTransactionsQueryHandler.handle(query)).thenReturn(expectedList);

        // When
        List<TransactionEntity> result = transactionService.getAccountTransactions(query);

        // Then
        assertThat(result).isEqualTo(expectedList);
        verify(getAccountTransactionsQueryHandler).handle(query);
    }

    @Test
    @DisplayName("should delegate deprecated initiateTransfer correctly")
    void shouldDelegateDeprecatedInitiateTransfer() {
        // Given
        InitiateTransferRequest request = InitiateTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("1000"))
                .type(InitiateTransferRequest.TransactionType.INTERNAL_TRANSFER)
                .description("Test")
                .build();
        String userId = "user-123";
        InitiateTransferCommandResult expectedResult = mock(InitiateTransferCommandResult.class);
        
        when(initiateTransferHandler.handle(any(InitiateTransferCommand.class))).thenReturn(expectedResult);

        // When
        InitiateTransferCommandResult result = transactionService.initiateTransfer(request, userId);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        verify(initiateTransferHandler).handle(any(InitiateTransferCommand.class));
    }
}
