package id.payu.transaction.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandHandler;
import id.payu.transaction.application.cqrs.command.ProcessQrisPaymentCommandHandler;
import id.payu.transaction.application.cqrs.query.GetAccountTransactionsQueryHandler;
import id.payu.transaction.application.cqrs.query.GetTransactionQueryHandler;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.port.out.TransactionPersistencePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
    @Mock
    private TransactionPersistencePort transactionPersistencePort;

    @Test
    void readsRecipientAccountNumberFromTransferMetadata() {
        UUID transactionId = UUID.randomUUID();
        TransactionEntity transaction = TransactionEntity.builder()
                .id(transactionId)
                .senderAccountId(UUID.randomUUID())
                .amount(Money.idr("100"))
                .metadata("{\"recipientAccountNumber\":\"1234567890\"}")
                .build();
        when(transactionPersistencePort.findById(transactionId)).thenReturn(Optional.of(transaction));

        TransactionService service = new TransactionService(
                initiateTransferHandler,
                processQrisPaymentHandler,
                getTransactionHandler,
                getAccountTransactionsQueryHandler,
                transactionPersistencePort,
                new ObjectMapper());

        var result = service.getTransactionRefundDetails(transactionId);

        assertThat(result.recipientAccountId()).isEqualTo("1234567890");
    }
}
