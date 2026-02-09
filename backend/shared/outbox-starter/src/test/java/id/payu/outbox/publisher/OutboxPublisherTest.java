package id.payu.outbox.publisher;

import id.payu.outbox.entity.OutboxEvent;
import id.payu.outbox.repository.OutboxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OutboxPublisher}.
 * Tests polling, publishing, retry/failure handling, and metrics.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxPublisher")
class OutboxPublisherTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private MeterRegistry meterRegistry;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, String>> recordCaptor;

    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        publisher = new OutboxPublisher(outboxRepository, kafkaTemplate, meterRegistry);

        // Set @Value fields via reflection
        ReflectionTestUtils.setField(publisher, "batchSize", 100);
        ReflectionTestUtils.setField(publisher, "maxRetries", 3);
        ReflectionTestUtils.setField(publisher, "defaultTopic", "outbox.events");
        ReflectionTestUtils.setField(publisher, "enabled", true);
        ReflectionTestUtils.setField(publisher, "lockTimeoutMs", 10000L);

        publisher.init();
    }

    private OutboxEvent createTestEvent() {
        return createTestEvent(null);
    }

    private OutboxEvent createTestEvent(String destinationTopic) {
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("Wallet")
                .aggregateId("wallet-001")
                .eventType("WalletCredited")
                .payload(Map.of("amount", 50000, "currency", "IDR"))
                .headers(Map.of("correlationId", "corr-123"))
                .destinationTopic(destinationTopic)
                .createdAt(Instant.now())
                .sequenceNum(1L)
                .retryCount(0)
                .build();
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<SendResult<String, String>> mockSuccessfulKafkaSend() {
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("outbox.events", 0), 0L, 0, 0L, 0, 0);
        SendResult<String, String> sendResult = new SendResult<>(
                new ProducerRecord<>("outbox.events", "key", "value"), metadata);
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);
        return future;
    }

    @Nested
    @DisplayName("init()")
    class InitTests {

        @Test
        @DisplayName("should register metrics gauges")
        void shouldRegisterMetricsGauges() {
            assertThat(meterRegistry.find("outbox.pending.events").gauge()).isNotNull();
            assertThat(meterRegistry.find("outbox.unpublished.count").gauge()).isNotNull();
        }
    }

    @Nested
    @DisplayName("pollAndPublish()")
    class PollAndPublishTests {

        @Test
        @DisplayName("should skip when disabled")
        void shouldSkipWhenDisabled() {
            ReflectionTestUtils.setField(publisher, "enabled", false);

            publisher.pollAndPublish();

            verify(outboxRepository, never()).findUnpublishedEventsWithLock(anyInt(), any(Pageable.class));
        }

        @Test
        @DisplayName("should return early when no unpublished events")
        void shouldReturnEarlyWhenEmpty() {
            when(outboxRepository.findUnpublishedEventsWithLock(anyInt(), any(Pageable.class)))
                    .thenReturn(Collections.emptyList());

            publisher.pollAndPublish();

            verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should publish all events in batch")
        void shouldPublishAllEventsInBatch() {
            OutboxEvent event1 = createTestEvent();
            OutboxEvent event2 = createTestEvent();

            when(outboxRepository.findUnpublishedEventsWithLock(anyInt(), any(Pageable.class)))
                    .thenReturn(List.of(event1, event2));
            mockSuccessfulKafkaSend();
            when(outboxRepository.markAsPublished(any(UUID.class), any(Instant.class))).thenReturn(1);

            publisher.pollAndPublish();

            verify(kafkaTemplate, times(2)).send(any(ProducerRecord.class));
            verify(outboxRepository, times(2)).markAsPublished(any(UUID.class), any(Instant.class));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should handle mixed success and failure in batch")
        void shouldHandleMixedResults() {
            OutboxEvent successEvent = createTestEvent();
            OutboxEvent failEvent = createTestEvent();

            when(outboxRepository.findUnpublishedEventsWithLock(anyInt(), any(Pageable.class)))
                    .thenReturn(List.of(successEvent, failEvent));

            // First call succeeds, second fails
            RecordMetadata metadata = new RecordMetadata(
                    new TopicPartition("outbox.events", 0), 0L, 0, 0L, 0, 0);
            SendResult<String, String> ok = new SendResult<>(
                    new ProducerRecord<>("outbox.events", "k", "v"), metadata);

            when(kafkaTemplate.send(any(ProducerRecord.class)))
                    .thenReturn(CompletableFuture.completedFuture(ok))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka down")));

            when(outboxRepository.markAsPublished(any(UUID.class), any(Instant.class))).thenReturn(1);

            publisher.pollAndPublish();

            // First event marked published, second event retried
            verify(outboxRepository, times(1)).markAsPublished(any(UUID.class), any(Instant.class));
            verify(outboxRepository, times(1)).incrementRetryCount(any(UUID.class), anyString());
        }
    }

    @Nested
    @DisplayName("publishEvent()")
    class PublishEventTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should publish to default topic when destinationTopic is null")
        void shouldPublishToDefaultTopic() {
            OutboxEvent event = createTestEvent(null); // null destination topic
            mockSuccessfulKafkaSend();
            when(outboxRepository.markAsPublished(any(UUID.class), any(Instant.class))).thenReturn(1);

            publisher.publishEvent(event);

            verify(kafkaTemplate).send(recordCaptor.capture());
            assertThat(recordCaptor.getValue().topic()).isEqualTo("outbox.events");
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should publish to custom destination topic")
        void shouldPublishToCustomTopic() {
            OutboxEvent event = createTestEvent("wallet.credits");

            RecordMetadata metadata = new RecordMetadata(
                    new TopicPartition("wallet.credits", 0), 0L, 0, 0L, 0, 0);
            SendResult<String, String> sendResult = new SendResult<>(
                    new ProducerRecord<>("wallet.credits", "k", "v"), metadata);
            when(kafkaTemplate.send(any(ProducerRecord.class)))
                    .thenReturn(CompletableFuture.completedFuture(sendResult));
            when(outboxRepository.markAsPublished(any(UUID.class), any(Instant.class))).thenReturn(1);

            publisher.publishEvent(event);

            verify(kafkaTemplate).send(recordCaptor.capture());
            assertThat(recordCaptor.getValue().topic()).isEqualTo("wallet.credits");
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should use aggregateId as Kafka message key")
        void shouldUseAggregateIdAsKey() {
            OutboxEvent event = createTestEvent();
            mockSuccessfulKafkaSend();
            when(outboxRepository.markAsPublished(any(UUID.class), any(Instant.class))).thenReturn(1);

            publisher.publishEvent(event);

            verify(kafkaTemplate).send(recordCaptor.capture());
            assertThat(recordCaptor.getValue().key()).isEqualTo("wallet-001");
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should add standard Kafka headers (eventId, eventType, aggregateType, sequenceNum)")
        void shouldAddStandardHeaders() {
            OutboxEvent event = createTestEvent();
            mockSuccessfulKafkaSend();
            when(outboxRepository.markAsPublished(any(UUID.class), any(Instant.class))).thenReturn(1);

            publisher.publishEvent(event);

            verify(kafkaTemplate).send(recordCaptor.capture());
            ProducerRecord<String, String> record = recordCaptor.getValue();

            assertThat(record.headers().lastHeader("eventId")).isNotNull();
            assertThat(record.headers().lastHeader("eventType")).isNotNull();
            assertThat(new String(record.headers().lastHeader("eventType").value()))
                    .isEqualTo("WalletCredited");
            assertThat(record.headers().lastHeader("aggregateType")).isNotNull();
            assertThat(new String(record.headers().lastHeader("aggregateType").value()))
                    .isEqualTo("Wallet");
            assertThat(record.headers().lastHeader("sequenceNum")).isNotNull();
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should add custom headers from event")
        void shouldAddCustomHeaders() {
            OutboxEvent event = createTestEvent();
            mockSuccessfulKafkaSend();
            when(outboxRepository.markAsPublished(any(UUID.class), any(Instant.class))).thenReturn(1);

            publisher.publishEvent(event);

            verify(kafkaTemplate).send(recordCaptor.capture());
            ProducerRecord<String, String> record = recordCaptor.getValue();

            assertThat(record.headers().lastHeader("correlationId")).isNotNull();
            assertThat(new String(record.headers().lastHeader("correlationId").value()))
                    .isEqualTo("corr-123");
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should mark event as published after successful send")
        void shouldMarkAsPublishedAfterSuccess() {
            OutboxEvent event = createTestEvent();
            mockSuccessfulKafkaSend();
            when(outboxRepository.markAsPublished(any(UUID.class), any(Instant.class))).thenReturn(1);

            publisher.publishEvent(event);

            verify(outboxRepository).markAsPublished(eq(event.getId()), any(Instant.class));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should record success metrics")
        void shouldRecordSuccessMetrics() {
            OutboxEvent event = createTestEvent();
            mockSuccessfulKafkaSend();
            when(outboxRepository.markAsPublished(any(UUID.class), any(Instant.class))).thenReturn(1);

            publisher.publishEvent(event);

            assertThat(meterRegistry.find("outbox.publish.success").counter()).isNotNull();
            assertThat(meterRegistry.find("outbox.publish.duration").timer()).isNotNull();
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should throw OutboxPublishException on Kafka failure")
        void shouldThrowOnKafkaFailure() {
            OutboxEvent event = createTestEvent();
            when(kafkaTemplate.send(any(ProducerRecord.class)))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Broker unavailable")));

            assertThatThrownBy(() -> publisher.publishEvent(event))
                    .isInstanceOf(OutboxPublisher.OutboxPublishException.class)
                    .hasMessageContaining(event.getId().toString());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should record failure metrics on error")
        void shouldRecordFailureMetrics() {
            OutboxEvent event = createTestEvent();
            when(kafkaTemplate.send(any(ProducerRecord.class)))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("fail")));

            try { publisher.publishEvent(event); } catch (Exception ignored) {}

            assertThat(meterRegistry.find("outbox.publish.failure").counter()).isNotNull();
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should serialize payload as JSON string")
        void shouldSerializePayloadAsJson() {
            OutboxEvent event = createTestEvent();
            mockSuccessfulKafkaSend();
            when(outboxRepository.markAsPublished(any(UUID.class), any(Instant.class))).thenReturn(1);

            publisher.publishEvent(event);

            verify(kafkaTemplate).send(recordCaptor.capture());
            String value = recordCaptor.getValue().value();

            assertThat(value).contains("\"amount\"");
            assertThat(value).contains("50000");
            assertThat(value).contains("\"currency\"");
            assertThat(value).contains("\"IDR\"");
        }
    }

    @Nested
    @DisplayName("publishEventById()")
    class PublishEventByIdTests {

        @Test
        @DisplayName("should throw OutboxEventNotFoundException when event not found")
        void shouldThrowWhenNotFound() {
            UUID eventId = UUID.randomUUID();
            when(outboxRepository.findWithLockById(eventId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> publisher.publishEventById(eventId))
                    .isInstanceOf(OutboxPublisher.OutboxEventNotFoundException.class)
                    .hasMessageContaining(eventId.toString());
        }

        @Test
        @DisplayName("should return false when event is already published")
        void shouldReturnFalseWhenAlreadyPublished() {
            OutboxEvent event = createTestEvent();
            event.setPublishedAt(Instant.now());
            when(outboxRepository.findWithLockById(event.getId())).thenReturn(Optional.of(event));

            boolean result = publisher.publishEventById(event.getId());

            assertThat(result).isFalse();
            verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should publish and return true when event unpublished")
        void shouldPublishAndReturnTrue() {
            OutboxEvent event = createTestEvent();
            when(outboxRepository.findWithLockById(event.getId())).thenReturn(Optional.of(event));
            mockSuccessfulKafkaSend();
            when(outboxRepository.markAsPublished(any(UUID.class), any(Instant.class))).thenReturn(1);

            boolean result = publisher.publishEventById(event.getId());

            assertThat(result).isTrue();
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should handle publish failure and return false")
        void shouldHandleFailureAndReturnFalse() {
            OutboxEvent event = createTestEvent();
            when(outboxRepository.findWithLockById(event.getId())).thenReturn(Optional.of(event));
            when(kafkaTemplate.send(any(ProducerRecord.class)))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("fail")));

            boolean result = publisher.publishEventById(event.getId());

            assertThat(result).isFalse();
            verify(outboxRepository).incrementRetryCount(eq(event.getId()), anyString());
        }
    }

    @Nested
    @DisplayName("retryFailedEvents()")
    class RetryFailedEventsTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should retry and count successfully published events")
        void shouldRetryAndCount() {
            OutboxEvent event1 = createTestEvent();
            OutboxEvent event2 = createTestEvent();
            Page<OutboxEvent> page = new PageImpl<>(List.of(event1, event2));

            when(outboxRepository.findUnpublishedEventsForRetry(anyInt(), any(Pageable.class)))
                    .thenReturn(page);
            mockSuccessfulKafkaSend();
            when(outboxRepository.markAsPublished(any(UUID.class), any(Instant.class))).thenReturn(1);

            int count = publisher.retryFailedEvents();

            assertThat(count).isEqualTo(2);
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should return 0 when no failed events")
        void shouldReturnZeroWhenEmpty() {
            Page<OutboxEvent> emptyPage = new PageImpl<>(Collections.emptyList());
            when(outboxRepository.findUnpublishedEventsForRetry(anyInt(), any(Pageable.class)))
                    .thenReturn(emptyPage);

            int count = publisher.retryFailedEvents();

            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("handlePublishFailure()")
    class HandlePublishFailureTests {

        @Test
        @DisplayName("should truncate error message to 1000 chars")
        void shouldTruncateErrorMessage() {
            OutboxEvent event = createTestEvent();
            String longMessage = "x".repeat(2000);
            Exception ex = new RuntimeException(longMessage);

            // handlePublishFailure is protected, invoke via publishEventById path
            when(outboxRepository.findWithLockById(event.getId())).thenReturn(Optional.of(event));
            when(kafkaTemplate.send(any(ProducerRecord.class)))
                    .thenReturn(CompletableFuture.failedFuture(ex));

            publisher.publishEventById(event.getId());

            ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
            verify(outboxRepository).incrementRetryCount(eq(event.getId()), errorCaptor.capture());

            assertThat(errorCaptor.getValue().length()).isLessThanOrEqualTo(1000);
        }

        @Test
        @DisplayName("should record permanent failure metric when max retries reached")
        void shouldRecordPermanentFailureMetric() {
            // Event with retryCount = 2 (maxRetries=3, so +1=3 >= 3 => permanent)
            OutboxEvent event = createTestEvent();
            event.setRetryCount(2);

            when(outboxRepository.findWithLockById(event.getId())).thenReturn(Optional.of(event));
            when(kafkaTemplate.send(any(ProducerRecord.class)))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("fail")));

            publisher.publishEventById(event.getId());

            assertThat(meterRegistry.find("outbox.publish.permanent.failure").counter()).isNotNull();
        }
    }
}
