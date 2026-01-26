package id.payu.transaction.application.service;

import id.payu.transaction.application.service.dto.ArchivalResult;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.model.TransactionArchive;
import id.payu.transaction.domain.port.out.TransactionArchivalPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentCaptor.forClass;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionArchivalService Test")
class TransactionArchivalServiceTest {

    @Mock
    private TransactionArchivalPersistencePort archivalPersistencePort;

    @InjectMocks
    private TransactionArchivalService archivalService;

    private Transaction completedTransaction;
    private Instant cutoffDate;

    @BeforeEach
    void setUp() {
        // Set private fields via reflection since @Value annotations aren't processed in Mockito tests
        ReflectionTestUtils.setField(archivalService, "retentionMonths", 12);
        ReflectionTestUtils.setField(archivalService, "batchSize", 1000);
        ReflectionTestUtils.setField(archivalService, "archivalEnabled", true);

        completedTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .referenceNumber("TXN1234567890")
                .senderAccountId(UUID.randomUUID())
                .recipientAccountId(UUID.randomUUID())
                .type(Transaction.TransactionType.INTERNAL_TRANSFER)
                .amount(new BigDecimal("100000"))
                .currency("IDR")
                .description("Test transfer")
                .status(Transaction.TransactionStatus.COMPLETED)
                .createdAt(ZonedDateTime.now().minusMonths(13).toInstant())
                .updatedAt(ZonedDateTime.now().minusMonths(13).toInstant())
                .completedAt(ZonedDateTime.now().minusMonths(13).toInstant())
                .build();

        cutoffDate = ZonedDateTime.now().minusMonths(12).toInstant();
    }

    @Test
    @DisplayName("should archive completed transactions older than retention period")
    void shouldArchiveCompletedTransactions() {
        given(archivalPersistencePort.countTransactionsToArchive(any(Instant.class))).willReturn(1L);
        given(archivalPersistencePort.getNextBatchId()).willReturn(1L);
        given(archivalPersistencePort.findTransactionsToArchive(any(Instant.class), anyInt()))
                .willReturn(List.of(completedTransaction));

        ArchivalResult result = archivalService.archiveOldTransactions();

        assertThat(result).isNotNull();
        assertThat(result.getArchivedCount()).isEqualTo(1);
        assertThat(result.getBatchId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");

        verify(archivalPersistencePort).archiveTransactions(anyList());
        verify(archivalPersistencePort).deleteArchivedTransactions(anyList());
    }

    @Test
    @DisplayName("should return NO_TRANSACTIONS status when no transactions to archive")
    void shouldReturnNoTransactionsStatus() {
        given(archivalPersistencePort.countTransactionsToArchive(any(Instant.class))).willReturn(0L);

        ArchivalResult result = archivalService.archiveOldTransactions();

        assertThat(result).isNotNull();
        assertThat(result.getArchivedCount()).isEqualTo(0);
        assertThat(result.getBatchId()).isNull();
        assertThat(result.getStatus()).isEqualTo("NO_TRANSACTIONS");

        verify(archivalPersistencePort, times(0)).archiveTransactions(anyList());
        verify(archivalPersistencePort, times(0)).deleteArchivedTransactions(anyList());
    }

    @Test
    @DisplayName("should return DISABLED status when archival is disabled")
    void shouldReturnDisabledStatus() {
        // Disable archival via reflection
        ReflectionTestUtils.setField(archivalService, "archivalEnabled", false);

        ArchivalResult result = archivalService.archiveOldTransactions();

        assertThat(result).isNotNull();
        assertThat(result.getArchivedCount()).isEqualTo(0);
        assertThat(result.getBatchId()).isNull();
        assertThat(result.getStatus()).isEqualTo("DISABLED");

        // Re-enable for other tests
        ReflectionTestUtils.setField(archivalService, "archivalEnabled", true);

        verify(archivalPersistencePort, times(0)).archiveTransactions(anyList());
        verify(archivalPersistencePort, times(0)).deleteArchivedTransactions(anyList());
    }

    @Test
    @DisplayName("should process multiple batches when transactions exceed batch size")
    void shouldProcessMultipleBatches() {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < 2500; i++) {
            transactions.add(Transaction.builder()
                    .id(UUID.randomUUID())
                    .referenceNumber("TXN" + i)
                    .senderAccountId(UUID.randomUUID())
                    .recipientAccountId(UUID.randomUUID())
                    .type(Transaction.TransactionType.INTERNAL_TRANSFER)
                    .amount(new BigDecimal("1000"))
                    .currency("IDR")
                    .status(Transaction.TransactionStatus.COMPLETED)
                    .createdAt(ZonedDateTime.now().minusMonths(13).toInstant())
                    .updatedAt(ZonedDateTime.now().minusMonths(13).toInstant())
                    .completedAt(ZonedDateTime.now().minusMonths(13).toInstant())
                    .build());
        }

        // Use AtomicInteger to track call count for pagination simulation
        AtomicInteger callCount = new AtomicInteger(0);

        given(archivalPersistencePort.countTransactionsToArchive(any(Instant.class))).willReturn(2500L);
        given(archivalPersistencePort.getNextBatchId()).willReturn(1L);
        given(archivalPersistencePort.findTransactionsToArchive(any(Instant.class), eq(1000)))
                .willAnswer(invocation -> {
                    int call = callCount.getAndIncrement();
                    int start = call * 1000;
                    int end = Math.min(start + 1000, transactions.size());
                    if (start >= transactions.size()) {
                        return List.of();
                    }
                    return transactions.subList(start, end);
                });

        ArchivalResult result = archivalService.archiveOldTransactions();

        assertThat(result.getArchivedCount()).isEqualTo(2500);
        assertThat(result.getBatchId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");

        verify(archivalPersistencePort, times(3)).archiveTransactions(anyList());
        verify(archivalPersistencePort, times(3)).deleteArchivedTransactions(anyList());
    }

    @Test
    @DisplayName("should return archived transactions for account")
    void shouldReturnArchivedTransactionsForAccount() {
        UUID accountId = UUID.randomUUID();
        List<TransactionArchive> archives = List.of(
                TransactionArchive.builder()
                        .id(UUID.randomUUID())
                        .referenceNumber("TXN1")
                        .senderAccountId(accountId)
                        .type(TransactionArchive.TransactionType.INTERNAL_TRANSFER)
                        .amount(new BigDecimal("1000"))
                        .currency("IDR")
                        .status(TransactionArchive.TransactionStatus.COMPLETED)
                        .archivedAt(Instant.now())
                        .archivalReason("RETENTION_EXPIRED")
                        .archivedBatchId(1L)
                        .build()
        );

        given(archivalPersistencePort.findByAccountId(eq(accountId), eq(0), eq(10)))
                .willReturn(archives);

        List<TransactionArchive> result = archivalService.getArchivedTransactions(accountId, 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSenderAccountId()).isEqualTo(accountId);
    }

    @Test
    @DisplayName("should return archived transactions by batch id")
    void shouldReturnArchivedTransactionsByBatchId() {
        Long batchId = 1L;
        List<TransactionArchive> archives = List.of(
                TransactionArchive.builder()
                        .id(UUID.randomUUID())
                        .referenceNumber("TXN1")
                        .archivedBatchId(batchId)
                        .archivalReason("RETENTION_EXPIRED")
                        .build()
        );

        given(archivalPersistencePort.findByBatchId(batchId)).willReturn(archives);

        List<TransactionArchive> result = archivalService.getArchivedTransactionsByBatch(batchId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getArchivedBatchId()).isEqualTo(batchId);
    }

    @Test
    @DisplayName("should count transactions to archive")
    void shouldCountTransactionsToArchive() {
        given(archivalPersistencePort.countTransactionsToArchive(any(Instant.class))).willReturn(100L);

        long count = archivalService.countTransactionsToArchive();

        assertThat(count).isEqualTo(100);
    }

    @Test
    @DisplayName("should convert transaction to archive correctly")
    void shouldConvertTransactionToArchiveCorrectly() {
        given(archivalPersistencePort.countTransactionsToArchive(any(Instant.class))).willReturn(1L);
        given(archivalPersistencePort.getNextBatchId()).willReturn(1L);
        given(archivalPersistencePort.findTransactionsToArchive(any(Instant.class), anyInt()))
                .willReturn(List.of(completedTransaction));

        archivalService.archiveOldTransactions();

        var archiveCaptor = forClass(List.class);
        verify(archivalPersistencePort).archiveTransactions(archiveCaptor.capture());

        @SuppressWarnings("unchecked")
        List<TransactionArchive> archives = archiveCaptor.getValue();
        assertThat(archives).hasSize(1);
        TransactionArchive archive = archives.get(0);
        assertThat(archive.getId()).isEqualTo(completedTransaction.getId());
        assertThat(archive.getReferenceNumber()).isEqualTo(completedTransaction.getReferenceNumber());
        assertThat(archive.getSenderAccountId()).isEqualTo(completedTransaction.getSenderAccountId());
        assertThat(archive.getRecipientAccountId()).isEqualTo(completedTransaction.getRecipientAccountId());
        assertThat(archive.getAmount()).isEqualTo(completedTransaction.getAmount());
        assertThat(archive.getCurrency()).isEqualTo(completedTransaction.getCurrency());
        assertThat(archive.getType().name()).isEqualTo(completedTransaction.getType().name());
        assertThat(archive.getStatus().name()).isEqualTo(completedTransaction.getStatus().name());
        assertThat(archive.getArchivalReason()).isEqualTo("RETENTION_EXPIRED");
        assertThat(archive.getArchivedBatchId()).isEqualTo(1L);
        assertThat(archive.getArchivedAt()).isNotNull();
    }
}
