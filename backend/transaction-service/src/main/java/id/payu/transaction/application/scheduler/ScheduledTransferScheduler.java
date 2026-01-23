package id.payu.transaction.application.scheduler;

import id.payu.transaction.domain.model.ScheduledTransfer;
import id.payu.transaction.domain.port.out.ScheduledTransferPersistencePort;
import id.payu.transaction.application.service.ScheduledTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTransferScheduler {

    private final ScheduledTransferPersistencePort persistencePort;
    private final ScheduledTransferService scheduledTransferService;

    @Scheduled(fixedRate = 60000)
    public void processDueScheduledTransfers() {
        try {
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
        }
    }
}
