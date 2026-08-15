package id.payu.transaction.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.outbox.service.OutboxService;
import id.payu.transaction.adapter.persistence.entity.BatchDisbursementEntity;
import id.payu.transaction.adapter.persistence.entity.DisbursementEntity;
import id.payu.transaction.domain.port.out.BatchDisbursementRepositoryPort;
import id.payu.transaction.domain.port.out.DisbursementRepositoryPort;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ARCH-TOPIC-003: batch disbursement must publish a batch-started outbox event
 * to the standard topic payu.transaction.disbursement-batch.v1 (the comment in
 * processBatch claimed it; it was never wired) and the listener must consume
 * the CloudEvents envelope from that topic.
 */
@ExtendWith(MockitoExtension.class)
class BatchDisbursementServiceTest {

    private static final String TOPIC = "payu.transaction.disbursement-batch.v1";

    @Mock
    private BatchDisbursementRepositoryPort batchRepository;
    @Mock
    private DisbursementRepositoryPort disbursementRepository;
    @Mock
    private DisbursementService disbursementService;
    @Mock
    private OutboxService outboxService;

    private BatchDisbursementService newService() {
        return new BatchDisbursementService(
                batchRepository, disbursementRepository, disbursementService, outboxService);
    }

    @Test
    void processBatchPublishesBatchStartedEventToStandardTopic() {
        UUID batchId = UUID.randomUUID();
        BatchDisbursementEntity batch = BatchDisbursementEntity.createWithIdempotencyKey(
                UUID.randomUUID(), "batch", "batch-" + UUID.randomUUID());
        batch.setId(batchId);

        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(BatchDisbursementEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BatchDisbursementService service = newService();
        service.processBatch(batchId);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(outboxService).createEvent(
                org.mockito.ArgumentMatchers.eq("BatchDisbursement"),
                org.mockito.ArgumentMatchers.eq(batchId.toString()),
                org.mockito.ArgumentMatchers.eq("BatchProcessingStarted"),
                payloadCaptor.capture(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(TOPIC));
        assertThat(payloadCaptor.getValue()).containsEntry("batchId", batchId.toString());
    }

    @Test
    void processBatchItemsConsumesCloudEventEnvelopeFromStandardTopic() throws Exception {
        UUID batchId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        BatchDisbursementEntity batch = BatchDisbursementEntity.createWithIdempotencyKey(
                UUID.randomUUID(), "batch", "batch-" + UUID.randomUUID());
        batch.setId(batchId);
        DisbursementEntity item = org.mockito.Mockito.mock(DisbursementEntity.class);
        when(item.isPending()).thenReturn(true);
        when(item.getId()).thenReturn(itemId);
        batch.setItems(List.of(item));

        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));

        BatchDisbursementService service = newService();
        String json = new ObjectMapper().writeValueAsString(Map.of(
                "specversion", "1.0.2",
                "id", UUID.randomUUID().toString(),
                "source", "/services/transaction-service",
                "type", "BatchProcessingStarted",
                "subject", batchId.toString(),
                "data", Map.of("batchId", batchId.toString())));
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(TOPIC, 0, 0L, batchId.toString(), json);

        service.processBatchItems(record);

        verify(disbursementService).processDisbursement(any(UUID.class));
    }

    @Test
    void processBatchItemsIgnoresMalformedPayload() {
        BatchDisbursementService service = newService();
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(TOPIC, 0, 0L, "key", "not-a-batch-id");

        service.processBatchItems(record);

        verify(batchRepository, never()).findById(any(UUID.class));
        verify(disbursementService, never()).processDisbursement(any(UUID.class));
    }
}
