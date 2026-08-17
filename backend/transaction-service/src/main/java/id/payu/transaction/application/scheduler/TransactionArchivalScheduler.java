package id.payu.transaction.application.scheduler;

import id.payu.transaction.interfaces.dto.ArchivalResult;
import id.payu.transaction.application.service.TransactionArchivalService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TransactionArchivalScheduler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TransactionArchivalScheduler.class);



    private final TransactionArchivalService archivalService;

    public TransactionArchivalScheduler(TransactionArchivalService archivalService) {
        this.archivalService = archivalService;
    }

    @SchedulerLock(name = "TransactionArchivalScheduler_archiveOldTransactions",
            lockAtLeastFor = "PT1S", lockAtMostFor = "PT4H")
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
