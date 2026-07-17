package id.payu.backoffice.adapter.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KycPiiBackfillRunner {
    private final KycPiiBackfillBatch backfillBatch;
    private final AtomicLong migrated = new AtomicLong();

    @Scheduled(initialDelayString = "${payu.kyc.pii-backfill.initial-delay-ms:10000}",
            fixedDelayString = "${payu.kyc.pii-backfill.fixed-delay-ms:60000}")
    public void runNextBatch() {
        try {
            int count = backfillBatch.migrateNextBatch();
            long total = migrated.addAndGet(count);
            if (count > 0) log.info("KYC PII backfill progress: batch={}, migratedSinceStart={}", count, total);
            else log.debug("KYC PII backfill is current: migratedSinceStart={}", total);
        } catch (RuntimeException exception) {
            log.warn("KYC PII backfill batch failed; scheduled retry will continue", exception);
        }
    }
}
