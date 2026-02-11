package id.payu.outbox;

import id.payu.outbox.entity.OutboxEvent;
import id.payu.outbox.repository.OutboxRepository;
import id.payu.outbox.service.OutboxService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Integration tests for the OutboxService and OutboxRepository.
 * <p>
 * These tests verify the transactional outbox pattern implementation
 * using an H2 in-memory database for fast execution.
 * <p>
 * Test cases cover:
 * <ul>
 *   <li>Saving outbox events</li>
 *   <li>Marking events as processed (published)</li>
 *   <li>Querying pending events</li>
 *   <li>Deleting old processed events</li>
 *   <li>Retry mechanism</li>
 * </ul>
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
@SpringBootTest(classes = TestConfig.class)
@ActiveProfiles("test")
@DisplayName("OutboxService Integration Tests")
class OutboxServiceIntegrationTest {

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxRepository outboxRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        // Clean up any existing data before each test
        outboxRepository.deleteAll();
    }

    // ─── Save Event Tests ───────────────────────────────────────────────

    @Nested
    @DisplayName("Save Outbox Event")
    @Transactional
    class SaveEventTests {

        @Test
        @DisplayName("Should save an outbox event and verify it's stored in database")
        void shouldSaveEventAndVerifyStored() {
            // Given
            String aggregateType = "Wallet";
            String aggregateId = "wallet-001";
            String eventType = "WalletCredited";
            Map<String, Object> payload = Map.of(
                    "walletId", "wallet-001",
                    "amount", 50000,
                    "currency", "IDR"
            );

            // When
            OutboxEvent savedEvent = outboxService.createEvent(aggregateType, aggregateId, eventType, payload);

            // Then
            assertThat(savedEvent).isNotNull();
            assertThat(savedEvent.getId()).isNotNull();
            assertThat(savedEvent.getAggregateType()).isEqualTo(aggregateType);
            assertThat(savedEvent.getAggregateId()).isEqualTo(aggregateId);
            assertThat(savedEvent.getEventType()).isEqualTo(eventType);
            assertThat(savedEvent.getPayload()).isEqualTo(payload);
            assertThat(savedEvent.getCreatedAt()).isNotNull();
            assertThat(savedEvent.getPublishedAt()).isNull();
            assertThat(savedEvent.getRetryCount()).isZero();

            // Verify it can be retrieved from database
            Optional<OutboxEvent> retrievedEvent = outboxRepository.findById(savedEvent.getId());
            assertThat(retrievedEvent).isPresent();
            assertThat(retrievedEvent.get().getAggregateType()).isEqualTo(aggregateType);
        }

        @Test
        @DisplayName("Should save event with custom headers and destination topic")
        void shouldSaveEventWithHeadersAndTopic() {
            // Given
            Map<String, Object> headers = Map.of(
                    "correlationId", "corr-123",
                    "traceId", "trace-456"
            );
            String destinationTopic = "wallet.credits";

            // When
            OutboxEvent savedEvent = outboxService.createEvent(
                    "Transaction",
                    "tx-001",
                    "TransactionCompleted",
                    Map.of("amount", 100000),
                    headers,
                    destinationTopic
            );

            // Then
            assertThat(savedEvent.getHeaders()).isEqualTo(headers);
            assertThat(savedEvent.getDestinationTopic()).isEqualTo(destinationTopic);

            // Verify from database
            OutboxEvent retrieved = outboxRepository.findById(savedEvent.getId()).orElseThrow();
            assertThat(retrieved.getHeaders()).containsEntry("correlationId", "corr-123");
            assertThat(retrieved.getDestinationTopic()).isEqualTo("wallet.credits");
        }

        @Test
        @DisplayName("Should save multiple events and verify count")
        void shouldSaveMultipleEvents() {
            // When
            for (int i = 0; i < 5; i++) {
                outboxService.createEvent(
                        "Wallet",
                        "wallet-" + i,
                        "WalletUpdated",
                        Map.of("index", i)
                );
            }

            // Then
            long count = outboxRepository.count();
            assertThat(count).isEqualTo(5);
        }
    }

    // ─── Mark as Published Tests ────────────────────────────────────────

    @Nested
    @DisplayName("Mark Event as Published")
    class MarkAsPublishedTests {

        @Test
        @DisplayName("Should mark an event as published using repository")
        @Transactional
        void shouldMarkEventAsPublished() {
            // Given
            OutboxEvent event = outboxService.createEvent(
                    "Wallet",
                    "wallet-001",
                    "WalletCredited",
                    Map.of("amount", 50000)
            );
            UUID eventId = event.getId();
            assertThat(event.isPublished()).isFalse();

            // Flush and clear to ensure we get fresh data from DB
            entityManager.flush();
            entityManager.clear();

            // When
            Instant publishedAt = Instant.now();
            int updated = outboxRepository.markAsPublished(eventId, publishedAt);

            // Flush the update
            entityManager.flush();
            entityManager.clear();

            // Then
            assertThat(updated).isEqualTo(1);

            OutboxEvent updatedEvent = outboxRepository.findById(eventId).orElseThrow();
            assertThat(updatedEvent.isPublished()).isTrue();
            assertThat(updatedEvent.getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should not update already published event")
        @Transactional
        void shouldNotUpdateAlreadyPublishedEvent() {
            // Given
            OutboxEvent event = outboxService.createEvent(
                    "Wallet",
                    "wallet-001",
                    "WalletCredited",
                    Map.of("amount", 50000)
            );
            Instant firstPublishTime = Instant.now().minusSeconds(60);

            entityManager.flush();
            entityManager.clear();

            outboxRepository.markAsPublished(event.getId(), firstPublishTime);
            entityManager.flush();
            entityManager.clear();

            // When - try to mark again
            Instant secondPublishTime = Instant.now();
            int updated = outboxRepository.markAsPublished(event.getId(), secondPublishTime);
            entityManager.flush();

            // Then - should not update because published_at is already set
            assertThat(updated).isEqualTo(0);

            OutboxEvent retrieved = outboxRepository.findById(event.getId()).orElseThrow();
            // Use isCloseTo to handle nanosecond precision differences between Java and H2
            assertThat(retrieved.getPublishedAt()).isCloseTo(firstPublishTime, within(1, ChronoUnit.MILLIS));
        }

        @Test
        @DisplayName("Should mark event as published using entity method")
        @Transactional
        void shouldMarkAsPublishedUsingEntityMethod() {
            // Given
            OutboxEvent event = outboxService.createEvent(
                    "Wallet",
                    "wallet-001",
                    "WalletCredited",
                    Map.of("amount", 50000)
            );

            // When
            event.markAsPublished();
            outboxRepository.save(event);

            // Then
            OutboxEvent retrieved = outboxRepository.findById(event.getId()).orElseThrow();
            assertThat(retrieved.isPublished()).isTrue();
            assertThat(retrieved.getPublishedAt()).isNotNull();
        }
    }

    // ─── Query Pending Events Tests ─────────────────────────────────────

    @Nested
    @DisplayName("Query Pending Events")
    @Transactional
    class QueryPendingEventsTests {

        @Test
        @DisplayName("Should find unpublished events")
        void shouldFindUnpublishedEvents() {
            // Given
            OutboxEvent unpublished1 = createUnpublishedEvent("Wallet", "w-1", "Event1");
            OutboxEvent unpublished2 = createUnpublishedEvent("Wallet", "w-2", "Event2");
            OutboxEvent published = createPublishedEvent("Wallet", "w-3", "Event3");

            entityManager.flush();
            entityManager.clear();

            // When
            Pageable pageable = PageRequest.of(0, 10);
            List<OutboxEvent> unpublished = outboxRepository.findUnpublishedEvents(pageable).getContent();

            // Then
            assertThat(unpublished).hasSize(2);
            assertThat(unpublished)
                    .extracting(OutboxEvent::getId)
                    .containsExactlyInAnyOrder(unpublished1.getId(), unpublished2.getId());
        }

        @Test
        @DisplayName("Should count unpublished events")
        void shouldCountUnpublishedEvents() {
            // Given
            createUnpublishedEvent("Wallet", "w-1", "Event1");
            createUnpublishedEvent("Wallet", "w-2", "Event2");
            createPublishedEvent("Wallet", "w-3", "Event3");

            entityManager.flush();
            entityManager.clear();

            // When
            long count = outboxRepository.countUnpublishedEvents();

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return pending event count from service")
        void shouldReturnPendingCountFromService() {
            // Given
            createUnpublishedEvent("Wallet", "w-1", "Event1");
            createUnpublishedEvent("Wallet", "w-2", "Event2");
            createPublishedEvent("Wallet", "w-3", "Event3");

            entityManager.flush();
            entityManager.clear();

            // When
            long count = outboxService.getPendingEventCount();

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should find unpublished events for retry")
        void shouldFindUnpublishedEventsForRetry() {
            // Given
            OutboxEvent event1 = createUnpublishedEvent("Wallet", "w-1", "Event1");
            event1.setRetryCount(1);
            outboxRepository.save(event1);

            OutboxEvent event2 = createUnpublishedEvent("Wallet", "w-2", "Event2");
            event2.setRetryCount(5); // Exceeds max retries
            outboxRepository.save(event2);

            entityManager.flush();
            entityManager.clear();

            // When
            Pageable pageable = PageRequest.of(0, 10);
            List<OutboxEvent> retryable = outboxRepository.findUnpublishedEventsForRetry(3, pageable).getContent();

            // Then
            assertThat(retryable).hasSize(1);
            assertThat(retryable.get(0).getId()).isEqualTo(event1.getId());
        }
    }

    // ─── Delete Old Events Tests ────────────────────────────────────────

    @Nested
    @DisplayName("Delete Old Processed Events")
    class DeleteOldEventsTests {

        @Test
        @DisplayName("Should delete published events older than cutoff date")
        @Transactional
        void shouldDeleteOldPublishedEvents() {
            // Given - Create events with specific timestamps
            Instant oldPublishedAt = Instant.now().minus(40, ChronoUnit.DAYS);
            OutboxEvent oldEvent = createPublishedEventWithTimestamp("Wallet", "w-1", "Event1", oldPublishedAt);

            Instant recentPublishedAt = Instant.now().minus(5, ChronoUnit.DAYS);
            OutboxEvent recentEvent = createPublishedEventWithTimestamp("Wallet", "w-2", "Event2", recentPublishedAt);

            entityManager.flush();
            entityManager.clear();

            // When
            Instant cutoffDate = Instant.now().minus(30, ChronoUnit.DAYS);
            int deleted = outboxRepository.deletePublishedEventsOlderThan(cutoffDate);
            entityManager.flush();

            // Then
            assertThat(deleted).isEqualTo(1);
            assertThat(outboxRepository.findById(oldEvent.getId())).isEmpty();
            assertThat(outboxRepository.findById(recentEvent.getId())).isPresent();
        }

        @Test
        @DisplayName("Should delete failed events older than cutoff date")
        @Transactional
        void shouldDeleteOldFailedEvents() {
            // Given
            Instant oldCreatedAt = Instant.now().minus(10, ChronoUnit.DAYS);
            OutboxEvent oldFailedEvent = createFailedEventAt("Wallet", "w-1", "Event1", oldCreatedAt, 5);

            Instant recentCreatedAt = Instant.now().minus(3, ChronoUnit.DAYS);
            OutboxEvent recentFailedEvent = createFailedEventAt("Wallet", "w-2", "Event2", recentCreatedAt, 5);

            entityManager.flush();
            entityManager.clear();

            // When
            Instant cutoffDate = Instant.now().minus(7, ChronoUnit.DAYS);
            int deleted = outboxRepository.deleteFailedEventsOlderThan(3, cutoffDate);
            entityManager.flush();

            // Then
            assertThat(deleted).isEqualTo(1);
            assertThat(outboxRepository.findById(oldFailedEvent.getId())).isEmpty();
            assertThat(outboxRepository.findById(recentFailedEvent.getId())).isPresent();
        }

        @Test
        @DisplayName("Should not delete unpublished events when deleting published")
        @Transactional
        void shouldNotDeleteUnpublishedEvents() {
            // Given
            OutboxEvent unpublished = createUnpublishedEvent("Wallet", "w-1", "Event1");
            OutboxEvent published = createPublishedEvent("Wallet", "w-2", "Event2");

            entityManager.flush();
            entityManager.clear();

            // When
            Instant cutoffDate = Instant.now().minus(1, ChronoUnit.DAYS);
            int deleted = outboxRepository.deletePublishedEventsOlderThan(cutoffDate);
            entityManager.flush();

            // Then
            assertThat(deleted).isEqualTo(0); // Published event is not old enough
            assertThat(outboxRepository.findById(unpublished.getId())).isPresent();
            assertThat(outboxRepository.findById(published.getId())).isPresent();
        }
    }

    // ─── Retry Mechanism Tests ──────────────────────────────────────────

    @Nested
    @DisplayName("Retry Mechanism")
    class RetryMechanismTests {

        @Test
        @DisplayName("Should increment retry count and set error message")
        @Transactional
        void shouldIncrementRetryCount() {
            // Given
            OutboxEvent event = outboxService.createEvent(
                    "Wallet",
                    "wallet-001",
                    "WalletCredited",
                    Map.of("amount", 50000)
            );
            UUID eventId = event.getId();

            entityManager.flush();
            entityManager.clear();

            // When
            int updated = outboxRepository.incrementRetryCount(eventId, "Connection timeout");
            entityManager.flush();
            entityManager.clear();

            // Then
            assertThat(updated).isEqualTo(1);

            OutboxEvent updatedEvent = outboxRepository.findById(eventId).orElseThrow();
            assertThat(updatedEvent.getRetryCount()).isEqualTo(1);
            assertThat(updatedEvent.getLastError()).isEqualTo("Connection timeout");
        }

        @Test
        @DisplayName("Should count failed events exceeding max retries")
        @Transactional
        void shouldCountFailedEvents() {
            // Given
            OutboxEvent failedEvent = createUnpublishedEvent("Wallet", "w-1", "Event1");
            failedEvent.setRetryCount(5);
            outboxRepository.save(failedEvent);

            OutboxEvent retryableEvent = createUnpublishedEvent("Wallet", "w-2", "Event2");
            retryableEvent.setRetryCount(2);
            outboxRepository.save(retryableEvent);

            entityManager.flush();
            entityManager.clear();

            // When
            long failedCount = outboxRepository.countFailedEvents(3);

            // Then
            assertThat(failedCount).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return failed event count from service")
        @Transactional
        void shouldReturnFailedCountFromService() {
            // Given
            OutboxEvent failedEvent = createUnpublishedEvent("Wallet", "w-1", "Event1");
            failedEvent.setRetryCount(5);
            outboxRepository.save(failedEvent);

            entityManager.flush();
            entityManager.clear();

            // When
            long count = outboxService.getFailedEventCount(3);

            // Then
            assertThat(count).isEqualTo(1);
        }
    }

    // ─── Find by Aggregate Tests ────────────────────────────────────────

    @Nested
    @DisplayName("Find Events by Aggregate")
    @Transactional
    class FindByAggregateTests {

        @Test
        @DisplayName("Should find events by aggregate type and ID")
        void shouldFindEventsByAggregateTypeAndId() {
            // Given
            String aggregateType = "Wallet";
            String aggregateId = "wallet-001";

            outboxService.createEvent(aggregateType, aggregateId, "WalletCreated", Map.of("step", 1));
            outboxService.createEvent(aggregateType, aggregateId, "WalletCredited", Map.of("step", 2));
            outboxService.createEvent(aggregateType, "wallet-002", "WalletCreated", Map.of("step", 3));
            outboxService.createEvent("Transaction", "tx-001", "TransactionCreated", Map.of("step", 4));

            entityManager.flush();
            entityManager.clear();

            // When
            Pageable pageable = PageRequest.of(0, 10);
            List<OutboxEvent> events = outboxRepository
                    .findByAggregateTypeAndAggregateIdOrderBySequenceNumAsc(aggregateType, aggregateId, pageable)
                    .getContent();

            // Then
            assertThat(events).hasSize(2);
            assertThat(events)
                    .extracting(OutboxEvent::getEventType)
                    .containsExactly("WalletCreated", "WalletCredited");
        }

        @Test
        @DisplayName("Should find events by aggregate type")
        void shouldFindEventsByAggregateType() {
            // Given
            outboxService.createEvent("Wallet", "w-1", "Event1", Map.of());
            outboxService.createEvent("Wallet", "w-2", "Event2", Map.of());
            outboxService.createEvent("Transaction", "tx-1", "Event3", Map.of());

            entityManager.flush();
            entityManager.clear();

            // When
            Pageable pageable = PageRequest.of(0, 10);
            List<OutboxEvent> walletEvents = outboxRepository
                    .findByAggregateTypeOrderBySequenceNumAsc("Wallet", pageable)
                    .getContent();

            // Then
            assertThat(walletEvents).hasSize(2);
        }
    }

    // ─── Helper Methods ─────────────────────────────────────────────────

    private OutboxEvent createUnpublishedEvent(String aggregateType, String aggregateId, String eventType) {
        return outboxService.createEvent(
                aggregateType,
                aggregateId,
                eventType,
                Map.of("data", UUID.randomUUID().toString())
        );
    }

    private OutboxEvent createPublishedEvent(String aggregateType, String aggregateId, String eventType) {
        OutboxEvent event = createUnpublishedEvent(aggregateType, aggregateId, eventType);
        event.markAsPublished();
        return outboxRepository.save(event);
    }

    private OutboxEvent createPublishedEventWithTimestamp(String aggregateType, String aggregateId, String eventType, Instant publishedAt) {
        // Create event with specific published timestamp using native query approach
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(Map.of("data", UUID.randomUUID().toString()))
                .createdAt(Instant.now().minus(41, ChronoUnit.DAYS))
                .publishedAt(publishedAt)
                .retryCount(0)
                .build();
        return outboxRepository.save(event);
    }

    private OutboxEvent createFailedEventAt(String aggregateType, String aggregateId, String eventType, Instant createdAt, int retryCount) {
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(Map.of("data", "test"))
                .createdAt(createdAt)
                .retryCount(retryCount)
                .lastError("Test error")
                .build();
        return outboxRepository.save(event);
    }
}
