package id.payu.partner.adapter.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * PARTNER-PROD-002: scheduled runner that migrates legacy plaintext partner
 * credentials and webhook secrets to ENC(...) ciphertext in resumable batches.
 * Graceful: a failed batch is retried on the next tick, and the
 * {@code FOR UPDATE SKIP LOCKED} query keeps multi-pod schedulers safe.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CredentialBackfillRunner {

    private final CredentialBackfillBatch backfillBatch;
    private final AtomicLong migrated = new AtomicLong();

    @Scheduled(initialDelayString = "${payu.partner.credential-backfill.initial-delay-ms:10000}",
            fixedDelayString = "${payu.partner.credential-backfill.fixed-delay-ms:60000}")
    public void runNextBatch() {
        try {
            int count = backfillBatch.migrateNextBatch();
            long total = migrated.addAndGet(count);
            if (count > 0) {
                log.info("Partner credential backfill progress: batch={}, migratedSinceStart={}",
                        count, total);
            } else {
                log.debug("Partner credential backfill is current: migratedSinceStart={}", total);
            }
        } catch (RuntimeException exception) {
            log.warn("Partner credential backfill batch failed; scheduled retry will continue",
                    exception);
        }
    }
}
