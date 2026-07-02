package id.payu.transaction.application.scheduler;

import id.payu.transaction.adapter.persistence.entity.ScheduledTransferEntity;
import id.payu.transaction.domain.port.out.ScheduledTransferPersistencePort;
import id.payu.transaction.application.service.ScheduledTransferService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Scheduler for processing due scheduled transfers (recurring + one-time).
 *
 * <p>ITER-53: Replaced manual Redis-based lock with {@code @SchedulerLock}
 * (ShedLock distributed lock on the DB). Other replicas skip execution
 * while one replica holds the lock. Lock auto-released after the method
 * completes or after {@code lockAtMostFor} elapses.</p>
 */
@Component
public class ScheduledTransferScheduler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ScheduledTransferScheduler.class);

    private final ScheduledTransferPersistencePort persistencePort;
    private final ScheduledTransferService scheduledTransferService;
    private final java.time.Clock clock;

    public ScheduledTransferScheduler(ScheduledTransferPersistencePort persistencePort,
                                      ScheduledTransferService scheduledTransferService,
                                      java.time.Clock clock) {
        this.persistencePort = persistencePort;
        this.scheduledTransferService = scheduledTransferService;
        this.clock = clock;
    }

    @SchedulerLock(name = "ScheduledTransferScheduler_processDueScheduledTransfers",
            lockAtLeastFor = "PT1S", lockAtMostFor = "PT55S")
    @Scheduled(fixedRate = 60000)
    public void processDueScheduledTransfers() {
        Instant now = Instant.now(clock);
        List<ScheduledTransferEntity> dueTransfers = persistencePort.findDueScheduledTransfers(now);

        if (dueTransfers.isEmpty()) {
            return;
        }

        log.info("Processing due scheduled transfers, count: {}", dueTransfers.size());

        for (ScheduledTransferEntity transfer : dueTransfers) {
            try {
                scheduledTransferService.processDueScheduledTransfer(transfer);
            } catch (Exception e) {
                log.error("Failed to process scheduled transfer, id: {}, error: {}",
                        transfer.getId(), e.getMessage());
            }
        }

        log.info("Completed processing due scheduled transfers, count: {}", dueTransfers.size());
    }
}
