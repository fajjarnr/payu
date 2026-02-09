package id.payu.outbox.scheduler;

import id.payu.outbox.config.OutboxProperties;
import id.payu.outbox.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OutboxCleanupScheduler}.
 * Verifies cleanup logic for published and failed events.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxCleanupScheduler")
class OutboxCleanupSchedulerTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OutboxProperties outboxProperties;

    @InjectMocks
    private OutboxCleanupScheduler scheduler;

    private OutboxProperties.CleanupProperties cleanupProperties;
    private OutboxProperties.PublisherProperties publisherProperties;

    @BeforeEach
    void setUp() {
        cleanupProperties = new OutboxProperties.CleanupProperties();
        cleanupProperties.setRetentionDays(30);
        cleanupProperties.setFailedRetentionDays(7);
        cleanupProperties.setEnabled(true);

        publisherProperties = new OutboxProperties.PublisherProperties();
        publisherProperties.setMaxRetries(3);

        lenient().when(outboxProperties.getCleanup()).thenReturn(cleanupProperties);
        lenient().when(outboxProperties.getPublisher()).thenReturn(publisherProperties);
    }

    @Nested
    @DisplayName("cleanupOldEvents()")
    class CleanupTests {

        @Test
        @DisplayName("should delete published events older than retention period")
        void shouldDeleteOldPublishedEvents() {
            when(outboxRepository.deletePublishedEventsOlderThan(any(Instant.class))).thenReturn(10);
            when(outboxRepository.deleteFailedEventsOlderThan(anyInt(), any(Instant.class))).thenReturn(0);

            scheduler.cleanupOldEvents();

            ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(outboxRepository).deletePublishedEventsOlderThan(cutoffCaptor.capture());

            Instant cutoff = cutoffCaptor.getValue();
            Instant expectedCutoff = Instant.now().minus(30, ChronoUnit.DAYS);
            // Allow 5 seconds tolerance for test execution time
            assertThat(cutoff).isBetween(expectedCutoff.minusSeconds(5), expectedCutoff.plusSeconds(5));
        }

        @Test
        @DisplayName("should delete failed events older than retention period with max retries")
        void shouldDeleteOldFailedEvents() {
            when(outboxRepository.deletePublishedEventsOlderThan(any(Instant.class))).thenReturn(0);
            when(outboxRepository.deleteFailedEventsOlderThan(anyInt(), any(Instant.class))).thenReturn(5);

            scheduler.cleanupOldEvents();

            ArgumentCaptor<Integer> retriesCaptor = ArgumentCaptor.forClass(Integer.class);
            ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(outboxRepository).deleteFailedEventsOlderThan(retriesCaptor.capture(), cutoffCaptor.capture());

            assertThat(retriesCaptor.getValue()).isEqualTo(3);

            Instant cutoff = cutoffCaptor.getValue();
            Instant expectedCutoff = Instant.now().minus(7, ChronoUnit.DAYS);
            assertThat(cutoff).isBetween(expectedCutoff.minusSeconds(5), expectedCutoff.plusSeconds(5));
        }

        @Test
        @DisplayName("should handle repository exception gracefully")
        void shouldHandleExceptionGracefully() {
            when(outboxRepository.deletePublishedEventsOlderThan(any(Instant.class)))
                    .thenThrow(new RuntimeException("DB connection failed"));

            // Should not throw
            scheduler.cleanupOldEvents();

            // Failed events deletion should not be called (exception occurred before)
            verify(outboxRepository, never()).deleteFailedEventsOlderThan(anyInt(), any(Instant.class));
        }

        @Test
        @DisplayName("should use configured retention days")
        void shouldUseConfiguredRetentionDays() {
            cleanupProperties.setRetentionDays(60);
            cleanupProperties.setFailedRetentionDays(14);

            when(outboxRepository.deletePublishedEventsOlderThan(any(Instant.class))).thenReturn(0);
            when(outboxRepository.deleteFailedEventsOlderThan(anyInt(), any(Instant.class))).thenReturn(0);

            scheduler.cleanupOldEvents();

            ArgumentCaptor<Instant> publishedCutoffCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(outboxRepository).deletePublishedEventsOlderThan(publishedCutoffCaptor.capture());

            Instant expectedPublished = Instant.now().minus(60, ChronoUnit.DAYS);
            assertThat(publishedCutoffCaptor.getValue())
                    .isBetween(expectedPublished.minusSeconds(5), expectedPublished.plusSeconds(5));
        }
    }
}
