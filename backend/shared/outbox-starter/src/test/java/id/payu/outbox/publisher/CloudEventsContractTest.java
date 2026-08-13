package id.payu.outbox.publisher;

import id.payu.outbox.entity.OutboxEvent;
import id.payu.outbox.repository.OutboxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * QAMVP-003: CloudEvents 1.0.2 wire contract for the outbox publisher. Every
 * published record must carry ce-specversion 1.0.2 + ce-id/source/type/time and
 * go to a {@code payu.<domain>.<event>.v<n>} topic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QAMVP-003 — CloudEvents 1.0.2 wire contract")
class CloudEventsContractTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, String>> recordCaptor;

    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        OutboxRepository repo = mock(OutboxRepository.class);
        MeterRegistry registry = new SimpleMeterRegistry();
        publisher = new OutboxPublisher(repo, kafkaTemplate, registry, mock(org.springframework.transaction.PlatformTransactionManager.class));
        ReflectionTestUtils.setField(publisher, "batchSize", 100);
        ReflectionTestUtils.setField(publisher, "maxRetries", 3);
        ReflectionTestUtils.setField(publisher, "defaultTopic", "outbox.events");
        ReflectionTestUtils.setField(publisher, "enabled", true);
        ReflectionTestUtils.setField(publisher, "lockTimeoutMs", 10000L);
        publisher.init();
    }

    private String header(ProducerRecord<String, String> record, String name) {
        var header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("published record carries CloudEvents 1.0.2 headers")
    void publishedRecordIsCloudEvents102() {
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("Wallet")
                .aggregateId("wallet-001")
                .eventType("WalletCredited")
                .payload(Map.of("amount", 50000, "currency", "IDR"))
                .destinationTopic("payu.wallet.credited.v1")
                .createdAt(Instant.parse("2026-08-13T00:00:00Z"))
                .sequenceNum(1L)
                .retryCount(0)
                .build();

        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(
                        new org.springframework.kafka.support.SendResult<>(
                                new ProducerRecord<>("payu.wallet.credited.v1", "k", "v"),
                                new org.apache.kafka.clients.producer.RecordMetadata(
                                        new org.apache.kafka.common.TopicPartition("payu.wallet.credited.v1", 0),
                                        0L, 0, 0L, 0, 0))));

        publisher.publishEvent(event);

        verify(kafkaTemplate).send(recordCaptor.capture());
        ProducerRecord<String, String> record = recordCaptor.getValue();

        assertThat(record.topic())
                .as("topic must be payu.<domain>.<event>.v<n>")
                .matches("^payu\\.[a-z][a-z0-9-]*\\.[a-z][a-z0-9-]*\\.v[0-9]+$");
        assertThat(header(record, "ce-specversion"))
                .as("CloudEvents specversion must be 1.0.2")
                .isEqualTo("1.0.2");
        assertThat(header(record, "ce-id")).isEqualTo(event.getId().toString());
        assertThat(header(record, "ce-source")).isEqualTo("/services/wallet-service");
        assertThat(header(record, "ce-type")).isEqualTo("WalletCredited");
        assertThat(header(record, "ce-time")).isNotNull();
        assertThat(record.value())
                .as("payload must be a CloudEvent envelope with data + specversion")
                .contains("\"specversion\":\"1.0.2\"")
                .contains("\"amount\"");
    }
}
