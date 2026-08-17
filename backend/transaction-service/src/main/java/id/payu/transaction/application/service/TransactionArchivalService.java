package id.payu.transaction.application.service;

import id.payu.transaction.interfaces.dto.ArchivalResult;
import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.adapter.persistence.entity.TransactionArchiveEntity;
import id.payu.transaction.domain.port.out.TransactionArchivalPersistencePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import id.payu.transaction.domain.model.TransactionArchiveStatus;
import id.payu.transaction.domain.model.TransactionArchiveType;

@Service
public class TransactionArchivalService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TransactionArchivalService.class);



    private final TransactionArchivalPersistencePort archivalPersistencePort;

    public TransactionArchivalService(TransactionArchivalPersistencePort archivalPersistencePort) {
        this.archivalPersistencePort = archivalPersistencePort;
    }

    @Value("${archival.retention-months:12}")
    private int retentionMonths;

    @Value("${archival.batch-size:1000}")
    private int batchSize;

    @Value("${archival.enabled:true}")
    private boolean archivalEnabled;

    /**
     * The orchestrator for archiving old transactions.
     * We don't use @Transactional here so that each batch can be strictly committed.
     * (Assuming the persistence port manages its own transactions for save/delete).
     */
    public ArchivalResult archiveOldTransactions() {
        if (!archivalEnabled) {
            log.info("TransactionEntity archival is disabled");
            return ArchivalResult.builder()
                    .archivedCount(0)
                    .batchId(null)
                    .status("DISABLED")
                    .build();
        }

        // BUG-BE-130: Use UTC explicitly for cutoff date calculation
        Instant cutoffDate = ZonedDateTime.now(ZoneId.of("UTC"))
                .minusMonths(retentionMonths)
                .toInstant();
                
        long totalToArchive = archivalPersistencePort.countTransactionsToArchive(cutoffDate);

        if (totalToArchive == 0) {
            log.info("No transactions to archive older than {}", cutoffDate);
            return ArchivalResult.builder()
                    .archivedCount(0)
                    .batchId(null)
                    .status("NO_TRANSACTIONS")
                    .build();
        }

        log.info("Starting archival of {} transactions older than {}", totalToArchive, cutoffDate);
        Long batchId = archivalPersistencePort.getNextBatchId();
        int totalArchived = 0;

        int processedBatches = 0;
        // BUG-BE-129: Infinite loop guard. Calculate max needed batches based on count and batchSize
        // We add a safety multiplier (e.g. * 2) in case more transactions became eligible during processing
        long maxBatches = ((totalToArchive / batchSize) + 1) * 2;
        
        while (processedBatches < maxBatches) {
            List<TransactionEntity> transactions = archivalPersistencePort.findTransactionsToArchive(cutoffDate, batchSize);

            if (transactions.isEmpty()) {
                break;
            }

            // BUG-BE-128: (Mitigation) In a full CQRS we'd split the archive and delete.
            // Assuming the persistence port has transaction control.
            List<TransactionArchiveEntity> archives = convertToArchives(transactions, batchId);
            archivalPersistencePort.archiveTransactions(archives);

            List<UUID> transactionIds = transactions.stream()
                    .map(TransactionEntity::getId)
                    .collect(Collectors.toList());
                    
            try {
                archivalPersistencePort.deleteArchivedTransactions(transactionIds);
            } catch (Exception e) {
                log.error("Failed to delete migrated origin transactions. Archival aborted to prevent duplicated records.", e);
                break;
            }

            totalArchived += transactions.size();
            processedBatches++;

            log.debug("Archived batch {}/{}: {} transactions in batch {}",
                    processedBatches, maxBatches, transactions.size(), batchId);

            if (transactions.size() < batchSize) {
                break;
            }
        }
        
        if (processedBatches >= maxBatches && totalArchived < totalToArchive) {
            log.warn("Archival stopped prematurely due to max batches guard limit reached. Suspected infinite loop.");
        }

        log.info("Completed archival of {} transactions in batch {}", totalArchived, batchId);
        return ArchivalResult.builder()
                .archivedCount(totalArchived)
                .batchId(batchId)
                .status("COMPLETED")
                .build();
    }

    public List<TransactionArchiveEntity> getArchivedTransactions(UUID accountId, int page, int size) {
        return archivalPersistencePort.findByAccountId(accountId, page, size);
    }

    public List<TransactionArchiveEntity> getArchivedTransactionsByBatch(Long batchId) {
        return archivalPersistencePort.findByBatchId(batchId);
    }

    public long countTransactionsToArchive() {
        Instant cutoffDate = ZonedDateTime.now(ZoneId.of("UTC")).minusMonths(retentionMonths).toInstant();
        return archivalPersistencePort.countTransactionsToArchive(cutoffDate);
    }

    private List<TransactionArchiveEntity> convertToArchives(List<TransactionEntity> transactions, Long batchId) {
        Instant archivedAt = Instant.now();
        List<TransactionArchiveEntity> archives = new ArrayList<>();

        for (TransactionEntity transaction : transactions) {
            TransactionArchiveEntity archive = TransactionArchiveEntity.builder()
                    .id(transaction.getId())
                    .referenceNumber(transaction.getReferenceNumber())
                    .senderAccountId(transaction.getSenderAccountId())
                    .recipientAccountId(transaction.getRecipientAccountId())
                    .type(TransactionArchiveType.valueOf(transaction.getType().name()))
                    .amount(transaction.getAmount().getAmount())
                    .currency(transaction.getAmount().getCurrency().getCurrencyCode())
                    .description(transaction.getDescription())
                    .status(TransactionArchiveStatus.valueOf(transaction.getStatus().name()))
                    .failureReason(transaction.getFailureReason())
                    .metadata(transaction.getMetadata())
                    .createdAt(transaction.getCreatedAt())
                    .updatedAt(transaction.getUpdatedAt())
                    .completedAt(transaction.getCompletedAt())
                    .archivedAt(archivedAt)
                    .archivalReason("RETENTION_EXPIRED")
                    .archivedBatchId(batchId)
                    .build();
            archives.add(archive);
        }

        return archives;
    }
}
