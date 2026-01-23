package id.payu.transaction.application.scheduler;

import id.payu.transaction.application.service.ArchivalResult;
import id.payu.transaction.application.service.TransactionArchivalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionArchivalScheduler {

    private final TransactionArchivalService archivalService;

    @Scheduled(cron = "${archival.schedule.cron:0 0 2 * * ?}")
    public void archiveOldTransactions() {
        log.info("Starting scheduled transaction archival");
        try {
            ArchivalResult result = archivalService.archiveOldTransactions();
            log.info("Scheduled archival completed: status={}, archivedCount={}, batchId={}",
                    result.getStatus(), result.getArchivedCount(), result.getBatchId());
        } catch (Exception e) {
            log.error("Error during scheduled transaction archival", e);
        }
    }
}
