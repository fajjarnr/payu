package id.payu.transaction.application.service;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.domain.model.TransactionStatus;
import id.payu.transaction.domain.port.in.TransactionUseCase;
import id.payu.transaction.dto.TransactionSummaryResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountTransactionSummaryServiceTest {

    private final UUID accountId = UUID.randomUUID();
    private final UUID otherAccount = UUID.randomUUID();

    private TransactionEntity tx(UUID sender, UUID recipient, String status, BigDecimal amount, Instant createdAt) {
        return TransactionEntity.builder()
                .id(UUID.randomUUID())
                .senderAccountId(sender)
                .recipientAccountId(recipient)
                .status(TransactionStatus.valueOf(status))
                .amountValue(amount)
                .currencyCode("IDR")
                .createdAt(createdAt)
                .build();
    }

    @Test
    void aggregatesTotalsSuccessFailureAndDateRange() {
        TransactionUseCase useCase = mock(TransactionUseCase.class);
        Instant oldest = Instant.parse("2026-01-01T00:00:00Z");
        Instant latest = Instant.parse("2026-03-01T00:00:00Z");
        when(useCase.getAccountTransactions(eq(accountId), eq("user-1"), any(Integer.class), any(Integer.class)))
                .thenReturn(List.of(
                        tx(accountId, otherAccount, "COMPLETED", new BigDecimal("100.0000"), latest),
                        tx(accountId, otherAccount, "FAILED", new BigDecimal("50.0000"), oldest),
                        tx(otherAccount, accountId, "COMPLETED", new BigDecimal("25.0000"), Instant.parse("2026-02-01T00:00:00Z"))
                ));

        AccountTransactionSummaryService service = new AccountTransactionSummaryService(useCase);
        TransactionSummaryResponse summary = service.summarize(accountId, "user-1");

        assertEquals(3, summary.totalTransactions());
        assertEquals(new BigDecimal("175.0000"), summary.totalAmount());
        assertEquals(new BigDecimal("150.0000"), summary.totalSent());
        assertEquals(new BigDecimal("25.0000"), summary.totalReceived());
        assertEquals(2, summary.successfulTransactions());
        assertEquals(1, summary.failedTransactions());
        assertEquals(oldest, summary.oldestTransactionDate());
        assertEquals(latest, summary.latestTransactionDate());
    }

    @Test
    void emptyHistoryYieldsZeroSummary() {
        TransactionUseCase useCase = mock(TransactionUseCase.class);
        when(useCase.getAccountTransactions(eq(accountId), eq("user-1"), any(Integer.class), any(Integer.class)))
                .thenReturn(List.of());

        AccountTransactionSummaryService service = new AccountTransactionSummaryService(useCase);
        TransactionSummaryResponse summary = service.summarize(accountId, "user-1");

        assertEquals(0, summary.totalTransactions());
        assertEquals(BigDecimal.ZERO, summary.totalAmount());
        assertEquals(null, summary.oldestTransactionDate());
    }
}
