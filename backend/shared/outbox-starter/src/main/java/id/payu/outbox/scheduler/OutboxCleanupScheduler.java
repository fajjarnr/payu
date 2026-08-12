package id.payu.outbox.scheduler;

import id.payu.outbox.config.OutboxProperties;
import id.payu.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduler for cleaning up old outbox events.
 * <p>
 * This component periodically removes published events older than the configured
 * retention period and ALERTS on failed events instead of deleting them
 * (OUTBOX-001): a failed event is a financial-integrity trace that must never
 * disappear silently — it stays archived in {@code outbox_events} and the
 * cleanup run emits an ERROR alert (count + cutoff) until an operator replays
 * it or moves it to the {@code .dlq} topic.
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
     * Scheduled cleanup task.
     * <p>
     * The schedule is configurable via {@code payu.outbox.cleanup.cron}.
     * Default: daily at 2 AM.
     */
    @SchedulerLock(name = "OutboxCleanupScheduler_cleanupOldEvents", lockAtLeastFor = "PT1S", lockAtMostFor = "PT1H")
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

            // OUTBOX-001: failed events are NEVER deleted — count them and alert
            // so a lost event always has a trace. Replay manually or move to the
            // .dlq topic; the archived rows are the audit record.
            // ponytail: log-level alert until the platform drift-alert destination
            // (Slack/PagerDuty via Vault) lands; same alert line can feed a
            // Prometheus log-rule alert.
            long failedArchived = outboxRepository.countFailedEventsOlderThan(
                    outboxProperties.getPublisher().getMaxRetries(), failedCutoff);
            if (failedArchived > 0) {
                log.error("OUTBOX-001 ALERT: {} failed events older than {} days remain archived in outbox_events — "
                                + "not deleted; replay them or move to the .dlq topic. cutoff={}, maxRetries={}",
                        failedArchived, outboxProperties.getCleanup().getFailedRetentionDays(),
                        failedCutoff, outboxProperties.getPublisher().getMaxRetries());
            }

            log.info("Outbox cleanup completed. Deleted published: {}, archived failed (alerted): {}",
                    publishedDeleted, failedArchived);

        } catch (Exception e) {
            log.error("Error during outbox cleanup", e);
        }
    }
}
