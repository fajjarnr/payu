package id.payu.outbox.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OutboxEvent} entity.
 * Validates entity behavior including state transitions, lifecycle callbacks,
 * and business logic methods.
 */
@DisplayName("OutboxEvent Entity")
class OutboxEventTest {

    @Nested
    @DisplayName("Builder and Defaults")
    class BuilderTests {

        @Test
        @DisplayName("should build event with all fields")
        void shouldBuildWithAllFields() {
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();
            Map<String, Object> payload = Map.of("amount", 100);
            Map<String, Object> headers = Map.of("correlationId", "abc-123");

            OutboxEvent event = OutboxEvent.builder()
                    .id(id)
                    .aggregateType("Wallet")
                    .aggregateId("wallet-001")
                    .eventType("WalletCredited")
                    .payload(payload)
                    .headers(headers)
                    .destinationTopic("wallet.events")
                    .createdAt(now)
                    .retryCount(0)
                    .build();

            assertThat(event.getId()).isEqualTo(id);
            assertThat(event.getAggregateType()).isEqualTo("Wallet");
            assertThat(event.getAggregateId()).isEqualTo("wallet-001");
            assertThat(event.getEventType()).isEqualTo("WalletCredited");
            assertThat(event.getPayload()).isEqualTo(payload);
            assertThat(event.getHeaders()).isEqualTo(headers);
            assertThat(event.getDestinationTopic()).isEqualTo("wallet.events");
            assertThat(event.getCreatedAt()).isEqualTo(now);
            assertThat(event.getRetryCount()).isZero();
            assertThat(event.getPublishedAt()).isNull();
            assertThat(event.getSequenceNum()).isNull();
            assertThat(event.getLastError()).isNull();
        }

        @Test
        @DisplayName("should default retryCount to 0 via @Builder.Default")
        void shouldDefaultRetryCountToZero() {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Transaction")
                    .aggregateId("tx-001")
                    .eventType("TransactionCreated")
                    .payload(Map.of("key", "value"))
                    .build();

            assertThat(event.getRetryCount()).isZero();
        }
    }

    @Nested
    @DisplayName("@PrePersist - onCreate()")
    class OnCreateTests {

        @Test
        @DisplayName("should set createdAt if null")
        void shouldSetCreatedAtIfNull() {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Wallet")
                    .aggregateId("w-1")
                    .eventType("WalletCreated")
                    .payload(Map.of())
                    .build();

            assertThat(event.getCreatedAt()).isNull();

            event.onCreate();

            assertThat(event.getCreatedAt()).isNotNull();
            assertThat(event.getCreatedAt()).isBefore(Instant.now().plusSeconds(1));
        }

        @Test
        @DisplayName("should not overwrite existing createdAt")
        void shouldNotOverwriteExistingCreatedAt() {
            Instant fixedTime = Instant.parse("2024-01-01T00:00:00Z");
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Wallet")
                    .aggregateId("w-1")
                    .eventType("WalletCreated")
                    .payload(Map.of())
                    .createdAt(fixedTime)
                    .build();

            event.onCreate();

            assertThat(event.getCreatedAt()).isEqualTo(fixedTime);
        }

        @Test
        @DisplayName("should set retryCount to 0 if null")
        void shouldSetRetryCountToZeroIfNull() {
            OutboxEvent event = new OutboxEvent();
            event.setRetryCount(null);

            event.onCreate();

            assertThat(event.getRetryCount()).isZero();
        }

        @Test
        @DisplayName("should not overwrite existing retryCount")
        void shouldNotOverwriteExistingRetryCount() {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Wallet")
                    .aggregateId("w-1")
                    .eventType("WalletCreated")
                    .payload(Map.of())
                    .retryCount(5)
                    .build();

            event.onCreate();

            assertThat(event.getRetryCount()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("markAsPublished()")
    class MarkAsPublishedTests {

        @Test
        @DisplayName("should set publishedAt timestamp")
        void shouldSetPublishedAt() {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Wallet")
                    .aggregateId("w-1")
                    .eventType("WalletCreated")
                    .payload(Map.of())
                    .build();

            assertThat(event.getPublishedAt()).isNull();

            Instant before = Instant.now();
            OutboxEvent result = event.markAsPublished();
            Instant after = Instant.now();

            assertThat(event.getPublishedAt()).isBetween(before, after);
            assertThat(result).isSameAs(event); // method chaining
        }

        @Test
        @DisplayName("should overwrite previous publishedAt (re-publish scenario)")
        void shouldOverwritePreviousPublishedAt() {
            Instant oldTime = Instant.parse("2024-01-01T00:00:00Z");
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Wallet")
                    .aggregateId("w-1")
                    .eventType("WalletCreated")
                    .payload(Map.of())
                    .publishedAt(oldTime)
                    .build();

            event.markAsPublished();

            assertThat(event.getPublishedAt()).isAfter(oldTime);
        }
    }

    @Nested
    @DisplayName("incrementRetry()")
    class IncrementRetryTests {

        @Test
        @DisplayName("should increment retryCount and set lastError")
        void shouldIncrementRetryCountAndSetError() {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Wallet")
                    .aggregateId("w-1")
                    .eventType("WalletCreated")
                    .payload(Map.of())
                    .retryCount(0)
                    .build();

            OutboxEvent result = event.incrementRetry("Connection timeout");

            assertThat(event.getRetryCount()).isEqualTo(1);
            assertThat(event.getLastError()).isEqualTo("Connection timeout");
            assertThat(result).isSameAs(event); // method chaining
        }

        @Test
        @DisplayName("should support multiple retries with different errors")
        void shouldSupportMultipleRetries() {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Wallet")
                    .aggregateId("w-1")
                    .eventType("WalletCreated")
                    .payload(Map.of())
                    .retryCount(0)
                    .build();

            event.incrementRetry("Error 1");
            event.incrementRetry("Error 2");
            event.incrementRetry("Error 3");

            assertThat(event.getRetryCount()).isEqualTo(3);
            assertThat(event.getLastError()).isEqualTo("Error 3");
        }
    }

    @Nested
    @DisplayName("isPublished()")
    class IsPublishedTests {

        @Test
        @DisplayName("should return false when publishedAt is null")
        void shouldReturnFalseWhenNotPublished() {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Wallet")
                    .aggregateId("w-1")
                    .eventType("WalletCreated")
                    .payload(Map.of())
                    .build();

            assertThat(event.isPublished()).isFalse();
        }

        @Test
        @DisplayName("should return true when publishedAt is set")
        void shouldReturnTrueWhenPublished() {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Wallet")
                    .aggregateId("w-1")
                    .eventType("WalletCreated")
                    .payload(Map.of())
                    .publishedAt(Instant.now())
                    .build();

            assertThat(event.isPublished()).isTrue();
        }
    }

    @Nested
    @DisplayName("shouldRetry()")
    class ShouldRetryTests {

        @Test
        @DisplayName("should return true when unpublished and under max retries")
        void shouldReturnTrueWhenRetriable() {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Wallet")
                    .aggregateId("w-1")
                    .eventType("WalletCreated")
                    .payload(Map.of())
                    .retryCount(1)
                    .build();

            assertThat(event.shouldRetry(3)).isTrue();
        }

        @Test
        @DisplayName("should return false when already published")
        void shouldReturnFalseWhenPublished() {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Wallet")
                    .aggregateId("w-1")
                    .eventType("WalletCreated")
                    .payload(Map.of())
                    .retryCount(0)
                    .publishedAt(Instant.now())
                    .build();

            assertThat(event.shouldRetry(3)).isFalse();
        }

        @Test
        @DisplayName("should return false when max retries reached")
        void shouldReturnFalseWhenMaxRetriesReached() {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Wallet")
                    .aggregateId("w-1")
                    .eventType("WalletCreated")
                    .payload(Map.of())
                    .retryCount(3)
                    .build();

            assertThat(event.shouldRetry(3)).isFalse();
        }

        @Test
        @DisplayName("should return false when retries exceed max")
        void shouldReturnFalseWhenRetriesExceedMax() {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Wallet")
                    .aggregateId("w-1")
                    .eventType("WalletCreated")
                    .payload(Map.of())
                    .retryCount(5)
                    .build();

            assertThat(event.shouldRetry(3)).isFalse();
        }

        @Test
        @DisplayName("should return true when retryCount is 0")
        void shouldReturnTrueWhenZeroRetries() {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("Wallet")
                    .aggregateId("w-1")
                    .eventType("WalletCreated")
                    .payload(Map.of())
                    .retryCount(0)
                    .build();

            assertThat(event.shouldRetry(3)).isTrue();
        }
    }
}
