package id.payu.transaction.application.service;

import id.payu.transaction.application.service.dto.ArchivalResult;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.model.TransactionArchive;
import id.payu.transaction.domain.port.out.TransactionArchivalPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionArchivalService {

    private final TransactionArchivalPersistencePort archivalPersistencePort;

    @Value("${archival.retention-months:12}")
    private int retentionMonths;

    @Value("${archival.batch-size:1000}")
    private int batchSize;

    @Value("${archival.enabled:true}")
    private boolean archivalEnabled;

    @Transactional
    public ArchivalResult archiveOldTransactions() {
        if (!archivalEnabled) {
            log.info("Transaction archival is disabled");
            return ArchivalResult.builder()
                    .archivedCount(0)
                    .batchId(null)
                    .status("DISABLED")
                    .build();
        }

        Instant cutoffDate = ZonedDateTime.now().minusMonths(retentionMonths).toInstant();
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
        while (true) {
            List<Transaction> transactions = archivalPersistencePort.findTransactionsToArchive(cutoffDate, batchSize);

            if (transactions.isEmpty()) {
                break;
            }

            List<TransactionArchive> archives = convertToArchives(transactions, batchId);
            archivalPersistencePort.archiveTransactions(archives);

            List<UUID> transactionIds = transactions.stream()
                    .map(Transaction::getId)
                    .collect(Collectors.toList());
            archivalPersistencePort.deleteArchivedTransactions(transactionIds);

            totalArchived += transactions.size();
            processedBatches++;

            log.debug("Archived batch {}/{}: {} transactions in batch {}",
                    processedBatches, (totalToArchive + batchSize - 1) / batchSize,
                    transactions.size(), batchId);

            if (transactions.size() < batchSize) {
                break;
            }
        }

        log.info("Completed archival of {} transactions in batch {}", totalArchived, batchId);
        return ArchivalResult.builder()
                .archivedCount(totalArchived)
                .batchId(batchId)
                .status("COMPLETED")
                .build();
    }

    public List<TransactionArchive> getArchivedTransactions(UUID accountId, int page, int size) {
        return archivalPersistencePort.findByAccountId(accountId, page, size);
    }

    public List<TransactionArchive> getArchivedTransactionsByBatch(Long batchId) {
        return archivalPersistencePort.findByBatchId(batchId);
    }

    public long countTransactionsToArchive() {
        Instant cutoffDate = ZonedDateTime.now().minusMonths(retentionMonths).toInstant();
        return archivalPersistencePort.countTransactionsToArchive(cutoffDate);
    }

    private List<TransactionArchive> convertToArchives(List<Transaction> transactions, Long batchId) {
        Instant archivedAt = Instant.now();
        List<TransactionArchive> archives = new ArrayList<>();

        for (Transaction transaction : transactions) {
            TransactionArchive archive = TransactionArchive.builder()
                    .id(transaction.getId())
                    .referenceNumber(transaction.getReferenceNumber())
                    .senderAccountId(transaction.getSenderAccountId())
                    .recipientAccountId(transaction.getRecipientAccountId())
                    .type(TransactionArchive.TransactionType.valueOf(transaction.getType().name()))
                    .amount(transaction.getAmount().getAmount())
                    .currency(transaction.getAmount().getCurrency().getCurrencyCode())
                    .description(transaction.getDescription())
                    .status(TransactionArchive.TransactionStatus.valueOf(transaction.getStatus().name()))
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
