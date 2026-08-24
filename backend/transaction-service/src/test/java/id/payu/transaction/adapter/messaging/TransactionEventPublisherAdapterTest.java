package id.payu.transaction.adapter.messaging;

import id.payu.transaction.application.service.DeferredOutboxService;
import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.model.TransactionStatus;
import id.payu.transaction.domain.model.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionEventPublisherAdapterTest {

    @Mock
    private DeferredOutboxService deferredOutboxService;

    @Test
    void publishesInitiatedTransactionToVersionedDomainTopic() {
        UUID transactionId = UUID.randomUUID();
        TransactionEntity transaction = TransactionEntity.builder()
                .id(transactionId)
                .referenceNumber("TXN-TOPIC-001")
                .senderAccountId(UUID.randomUUID())
                .amount(Money.idr("100"))
                .type(TransactionType.INTERNAL_TRANSFER)
                .status(TransactionStatus.PENDING)
                .createdAt(Instant.parse("2026-08-04T00:00:00Z"))
                .build();

        new TransactionEventPublisherAdapter(deferredOutboxService).publishTransactionInitiated(transaction);

        verify(deferredOutboxService).publishAfterCommit(
                eq("TransactionEntity"),
                eq(transactionId.toString()),
                eq("TransactionInitiated"),
                anyMap(),
                eq("payu.transaction.initiated.v1"));
    }
}
