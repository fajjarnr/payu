package id.payu.transaction.application.scheduler;

import id.payu.transaction.domain.model.ScheduledTransfer;
import id.payu.transaction.domain.port.out.ScheduledTransferPersistencePort;
import id.payu.transaction.application.service.ScheduledTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ScheduledTransferScheduler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ScheduledTransferScheduler.class);



    private final ScheduledTransferPersistencePort persistencePort;
    private final ScheduledTransferService scheduledTransferService;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    public ScheduledTransferScheduler(ScheduledTransferPersistencePort persistencePort,
                                      ScheduledTransferService scheduledTransferService,
                                      org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate) {
        this.persistencePort = persistencePort;
        this.scheduledTransferService = scheduledTransferService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Scheduled(fixedRate = 60000)
    public void processDueScheduledTransfers() {
        String lockKey = "lock:scheduled_transfers";
        String lockValue = java.util.UUID.randomUUID().toString();
        boolean lockAcquired = false;
        
        try {
            // Try to acquire lock for 55 seconds (slightly less than the schedule rate)
            Boolean result = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, java.time.Duration.ofSeconds(55));
            lockAcquired = Boolean.TRUE.equals(result);

            if (!lockAcquired) {
                log.debug("Another instance is processing scheduled transfers, skipping...");
                return;
            }

            Instant now = Instant.now();
            List<ScheduledTransfer> dueTransfers = persistencePort.findDueScheduledTransfers(now);

            if (dueTransfers.isEmpty()) {
                return;
            }

            log.info("Processing due scheduled transfers, count: {}", dueTransfers.size());

            for (ScheduledTransfer transfer : dueTransfers) {
                try {
                    scheduledTransferService.processDueScheduledTransfer(transfer);
                } catch (Exception e) {
                    log.error("Failed to process scheduled transfer, id: {}, error: {}", 
                            transfer.getId(), e.getMessage());
                }
            }

            log.info("Completed processing due scheduled transfers, count: {}", dueTransfers.size());

        } catch (Exception e) {
            log.error("Error processing due scheduled transfers, error: {}", e.getMessage());
        } finally {
            if (lockAcquired) {
                try {
                    // Only release the lock if we hold it
                    String currentValue = stringRedisTemplate.opsForValue().get(lockKey);
                    if (lockValue.equals(currentValue)) {
                        stringRedisTemplate.delete(lockKey);
                    }
                } catch (Exception e) {
                    log.error("Failed to release scheduled transfer lock: {}", e.getMessage());
                }
            }
        }
    }
}
