package id.payu.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.outbox.entity.OutboxEvent;
import id.payu.outbox.repository.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OutboxService}.
 * Tests event creation, validation, serialization, and count queries.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxService")
class OutboxServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxService outboxService;

    @Captor
    private ArgumentCaptor<OutboxEvent> eventCaptor;

    private static final String AGGREGATE_TYPE = "Wallet";
    private static final String AGGREGATE_ID = "wallet-001";
    private static final String EVENT_TYPE = "WalletCredited";
    private static final Map<String, Object> PAYLOAD = Map.of("amount", 50000, "currency", "IDR");

    private void stubSave() {
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> {
            OutboxEvent event = invocation.getArgument(0);
            if (event.getId() == null) {
                event.setId(UUID.randomUUID());
            }
            return event;
        });
    }

    @Nested
    @DisplayName("createEvent() - basic overload")
    class CreateEventBasicTests {

        @Test
        @DisplayName("should create event with required fields only")
        void shouldCreateEventWithRequiredFields() {
            stubSave();
            OutboxEvent result = outboxService.createEvent(AGGREGATE_TYPE, AGGREGATE_ID, EVENT_TYPE, PAYLOAD);

            verify(outboxRepository).save(eventCaptor.capture());
            OutboxEvent saved = eventCaptor.getValue();

            assertThat(saved.getAggregateType()).isEqualTo(AGGREGATE_TYPE);
            assertThat(saved.getAggregateId()).isEqualTo(AGGREGATE_ID);
            assertThat(saved.getEventType()).isEqualTo(EVENT_TYPE);
            assertThat(saved.getPayload()).isEqualTo(PAYLOAD);
            assertThat(saved.getHeaders()).isNull();
            assertThat(saved.getDestinationTopic()).isNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getRetryCount()).isZero();
            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("createEvent() - with headers")
    class CreateEventWithHeadersTests {

        @Test
        @DisplayName("should create event with custom headers")
        void shouldCreateEventWithHeaders() {
            stubSave();
            Map<String, Object> headers = Map.of("correlationId", "corr-123", "traceId", "trace-456");

            OutboxEvent result = outboxService.createEvent(AGGREGATE_TYPE, AGGREGATE_ID, EVENT_TYPE, PAYLOAD, headers);

            verify(outboxRepository).save(eventCaptor.capture());
            OutboxEvent saved = eventCaptor.getValue();

            assertThat(saved.getHeaders()).isEqualTo(headers);
        }
    }

    @Nested
    @DisplayName("createEvent() - full overload")
    class CreateEventFullTests {

        @Test
        @DisplayName("should create event with headers and destination topic")
        void shouldCreateEventWithHeadersAndTopic() {
            stubSave();
            Map<String, Object> headers = Map.of("correlationId", "corr-123");
            String topic = "wallet.credits";

            OutboxEvent result = outboxService.createEvent(
                    AGGREGATE_TYPE, AGGREGATE_ID, EVENT_TYPE, PAYLOAD, headers, topic);

            verify(outboxRepository).save(eventCaptor.capture());
            OutboxEvent saved = eventCaptor.getValue();

            assertThat(saved.getHeaders()).isEqualTo(headers);
            assertThat(saved.getDestinationTopic()).isEqualTo(topic);
        }

        @Test
        @DisplayName("should allow null headers and topic")
        void shouldAllowNullHeadersAndTopic() {
            stubSave();
            OutboxEvent result = outboxService.createEvent(
                    AGGREGATE_TYPE, AGGREGATE_ID, EVENT_TYPE, PAYLOAD, null, null);

            verify(outboxRepository).save(eventCaptor.capture());
            OutboxEvent saved = eventCaptor.getValue();

            assertThat(saved.getHeaders()).isNull();
            assertThat(saved.getDestinationTopic()).isNull();
        }
    }

    @Nested
    @DisplayName("Null validation")
    class NullValidationTests {

        @Test
        @DisplayName("should throw NullPointerException for null aggregateType")
        void shouldThrowForNullAggregateType() {
            assertThatThrownBy(() ->
                    outboxService.createEvent(null, AGGREGATE_ID, EVENT_TYPE, PAYLOAD))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("aggregateType");
        }

        @Test
        @DisplayName("should throw NullPointerException for null aggregateId")
        void shouldThrowForNullAggregateId() {
            assertThatThrownBy(() ->
                    outboxService.createEvent(AGGREGATE_TYPE, null, EVENT_TYPE, PAYLOAD))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("aggregateId");
        }

        @Test
        @DisplayName("should throw NullPointerException for null eventType")
        void shouldThrowForNullEventType() {
            assertThatThrownBy(() ->
                    outboxService.createEvent(AGGREGATE_TYPE, AGGREGATE_ID, null, PAYLOAD))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("eventType");
        }

        @Test
        @DisplayName("should throw NullPointerException for null payload")
        void shouldThrowForNullPayload() {
            assertThatThrownBy(() ->
                    outboxService.createEvent(AGGREGATE_TYPE, AGGREGATE_ID, EVENT_TYPE, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("payload");
        }
    }

    @Nested
    @DisplayName("createEventFromObject()")
    class CreateEventFromObjectTests {

        @Test
        @DisplayName("should serialize domain object to map payload")
        void shouldSerializeDomainObject() {
            stubSave();
            record WalletEvent(String walletId, int amount, String currency) {}
            WalletEvent domainEvent = new WalletEvent("wallet-001", 50000, "IDR");
            when(objectMapper.convertValue(any(WalletEvent.class), eq(Map.class)))
                    .thenReturn(Map.of("walletId", "wallet-001", "amount", 50000, "currency", "IDR"));

            OutboxEvent result = outboxService.createEventFromObject(
                    AGGREGATE_TYPE, AGGREGATE_ID, EVENT_TYPE, domainEvent);

            verify(outboxRepository).save(eventCaptor.capture());
            OutboxEvent saved = eventCaptor.getValue();

            assertThat(saved.getPayload()).containsEntry("walletId", "wallet-001");
            assertThat(saved.getPayload()).containsEntry("amount", 50000);
            assertThat(saved.getPayload()).containsEntry("currency", "IDR");
        }

        @Test
        @DisplayName("should serialize domain object with headers and topic")
        void shouldSerializeDomainObjectWithHeadersAndTopic() {
            stubSave();
            record TransferEvent(String from, String to, int amount) {}
            TransferEvent domainEvent = new TransferEvent("acc-1", "acc-2", 100000);
            Map<String, Object> headers = Map.of("traceId", "trace-789");
            when(objectMapper.convertValue(any(TransferEvent.class), eq(Map.class)))
                    .thenReturn(Map.of("from", "acc-1", "to", "acc-2", "amount", 100000));

            OutboxEvent result = outboxService.createEventFromObject(
                    "Transfer", "transfer-001", "TransferCompleted", domainEvent, headers, "transfer.events");

            verify(outboxRepository).save(eventCaptor.capture());
            OutboxEvent saved = eventCaptor.getValue();

            assertThat(saved.getPayload()).containsEntry("from", "acc-1");
            assertThat(saved.getPayload()).containsEntry("to", "acc-2");
            assertThat(saved.getHeaders()).isEqualTo(headers);
            assertThat(saved.getDestinationTopic()).isEqualTo("transfer.events");
        }

        @Test
        @DisplayName("should throw OutboxEventCreationException for unserializable object")
        void shouldThrowForUnserializableObject() {
            // Create an object that Jackson cannot convert to Map
            Object badObject = new Object() {
                // Self-referencing getter that causes infinite recursion
                public Object getSelf() { return this; }
            };
            when(objectMapper.convertValue(any(), eq(Map.class)))
                    .thenThrow(new IllegalArgumentException("Cannot serialize"));

            assertThatThrownBy(() -> outboxService.createEventFromObject(
                    AGGREGATE_TYPE, AGGREGATE_ID, EVENT_TYPE, badObject))
                    .isInstanceOf(OutboxService.OutboxEventCreationException.class)
                    .hasMessageContaining("Failed to serialize domain event");
        }
    }

    @Nested
    @DisplayName("Count queries")
    class CountQueryTests {

        @Test
        @DisplayName("should delegate getPendingEventCount to repository")
        void shouldDelegateGetPendingCount() {
            when(outboxRepository.countUnpublishedEvents()).thenReturn(42L);

            long count = outboxService.getPendingEventCount();

            assertThat(count).isEqualTo(42L);
            verify(outboxRepository).countUnpublishedEvents();
        }

        @Test
        @DisplayName("should delegate getFailedEventCount to repository")
        void shouldDelegateGetFailedCount() {
            when(outboxRepository.countFailedEvents(3)).thenReturn(5L);

            long count = outboxService.getFailedEventCount(3);

            assertThat(count).isEqualTo(5L);
            verify(outboxRepository).countFailedEvents(3);
        }
    }

    @Nested
    @DisplayName("Repository interaction")
    class RepositoryInteractionTests {

        @Test
        @DisplayName("should call repository.save exactly once per createEvent")
        void shouldCallSaveOnce() {
            stubSave();
            outboxService.createEvent(AGGREGATE_TYPE, AGGREGATE_ID, EVENT_TYPE, PAYLOAD);

            verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
            verifyNoMoreInteractions(outboxRepository);
        }

        @Test
        @DisplayName("should set createdAt before saving")
        void shouldSetCreatedAtBeforeSave() {
            stubSave();
            Instant before = Instant.now();
            outboxService.createEvent(AGGREGATE_TYPE, AGGREGATE_ID, EVENT_TYPE, PAYLOAD);

            verify(outboxRepository).save(eventCaptor.capture());
            Instant createdAt = eventCaptor.getValue().getCreatedAt();

            assertThat(createdAt).isAfterOrEqualTo(before);
            assertThat(createdAt).isBeforeOrEqualTo(Instant.now());
        }
    }
}
