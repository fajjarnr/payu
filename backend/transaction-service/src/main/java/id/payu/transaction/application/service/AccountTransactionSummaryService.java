package id.payu.transaction.application.service;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import id.payu.transaction.domain.model.TransactionStatus;
import id.payu.transaction.domain.port.in.TransactionUseCase;
import id.payu.transaction.interfaces.dto.TransactionSummaryResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Computes a per-account transaction summary (GRPC-008) by aggregating the
 * account transaction history. ponytail: page-scan over the whole history —
 * switch to a single SQL aggregate in TransactionJpaRepository when per-account
 * volume or scoring frequency outgrows this.
 */
@Service
public class AccountTransactionSummaryService {

    private static final int PAGE_SIZE = 100;

    private final TransactionUseCase transactionUseCase;

    public AccountTransactionSummaryService(TransactionUseCase transactionUseCase) {
        this.transactionUseCase = transactionUseCase;
    }

    public TransactionSummaryResponse summarize(UUID accountId, String userId) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalSent = BigDecimal.ZERO;
        BigDecimal totalReceived = BigDecimal.ZERO;
        long total = 0;
        long successful = 0;
        long failed = 0;
        Instant oldest = null;
        Instant latest = null;

        int page = 0;
        while (true) {
            List<TransactionEntity> batch = transactionUseCase.getAccountTransactions(
                    accountId, userId, page, PAGE_SIZE);
            if (batch.isEmpty()) {
                break;
            }
            for (TransactionEntity tx : batch) {
                BigDecimal amount = tx.getAmountValue() != null ? tx.getAmountValue() : BigDecimal.ZERO;
                totalAmount = totalAmount.add(amount);
                if (accountId.equals(tx.getSenderAccountId())) {
                    totalSent = totalSent.add(amount);
                }
                if (accountId.equals(tx.getRecipientAccountId())) {
                    totalReceived = totalReceived.add(amount);
                }
                if (tx.getStatus() == TransactionStatus.COMPLETED) {
                    successful++;
                } else if (tx.getStatus() == TransactionStatus.FAILED) {
                    failed++;
                }
                total++;
                if (tx.getCreatedAt() != null) {
                    if (oldest == null || tx.getCreatedAt().isBefore(oldest)) {
                        oldest = tx.getCreatedAt();
                    }
                    if (latest == null || tx.getCreatedAt().isAfter(latest)) {
                        latest = tx.getCreatedAt();
                    }
                }
            }
            if (batch.size() < PAGE_SIZE) {
                break;
            }
            page++;
        }

        return new TransactionSummaryResponse(
                accountId, total, totalAmount, totalSent, totalReceived,
                successful, failed, oldest, latest);
    }
}
