package id.payu.outbox.scheduler;

import id.payu.outbox.config.OutboxProperties;
import id.payu.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduler for cleaning up old outbox events.
 * <p>
 * This component periodically removes published and failed events that are older
 * than the configured retention period. This prevents the outbox table from
 * growing indefinitely.
 * <p>
 * The cleanup is disabled by default and can be enabled via configuration:
 * <pre>{@code
 * payu:
 *   outbox:
 *     cleanup:
 *       enabled: true
 *       retention-days: 30
 *       failed-retention-days: 7
 *       cron: "0 0 2 * * *"
 * }</pre>
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "payu.outbox.cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxCleanupScheduler {

    private final OutboxRepository outboxRepository;
    private final OutboxProperties outboxProperties;

    /**
     * Scheduled cleanup task that removes old published and failed events.
     * <p>
     * The schedule is configurable via {@code payu.outbox.cleanup.cron}.
     * Default: daily at 2 AM.
     */
    @Scheduled(cron = "${payu.outbox.cleanup.cron:0 0 2 * * *}")
    @Transactional
    public void cleanupOldEvents() {
        log.info("Starting outbox cleanup job");

        try {
            // Calculate cutoff dates
            Instant publishedCutoff = Instant.now()
                    .minus(outboxProperties.getCleanup().getRetentionDays(), ChronoUnit.DAYS);
            Instant failedCutoff = Instant.now()
                    .minus(outboxProperties.getCleanup().getFailedRetentionDays(), ChronoUnit.DAYS);

            // Delete old published events
            int publishedDeleted = outboxRepository.deletePublishedEventsOlderThan(publishedCutoff);
            if (publishedDeleted > 0) {
                log.info("Deleted {} published events older than {} days",
                        publishedDeleted, outboxProperties.getCleanup().getRetentionDays());
            }

            // Delete old failed events
            int failedDeleted = outboxRepository.deleteFailedEventsOlderThan(
                    outboxProperties.getPublisher().getMaxRetries(), failedCutoff);
            if (failedDeleted > 0) {
                log.info("Deleted {} failed events older than {} days",
                        failedDeleted, outboxProperties.getCleanup().getFailedRetentionDays());
            }

            log.info("Outbox cleanup completed. Total deleted: {}", publishedDeleted + failedDeleted);

        } catch (Exception e) {
            log.error("Error during outbox cleanup", e);
        }
    }
}
