package id.payu.investment.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder
public class InvestmentOperation {

    private UUID id;
    private String idempotencyKey;
    private String accountId;
    private String userId;
    private InvestmentOperationType operationType;
    private String productCode;
    private Integer tenure;
    private BigDecimal amount;
    private BigDecimal price;
    private String currency;
    private InvestmentOperationStatus status;
    private UUID targetId;
    private String debitReference;
    private String compensationReference;
    private String failureReason;
    @Builder.Default
    private Integer retryCount = 0;
    private LocalDateTime nextAttemptAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public static InvestmentOperation requested(
            String idempotencyKey,
            String accountId,
            String userId,
            InvestmentOperationType operationType,
            String productCode,
            Integer tenure,
            BigDecimal amount,
            BigDecimal price,
            String currency) {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        return InvestmentOperation.builder()
                .id(id)
                .idempotencyKey(idempotencyKey)
                .accountId(accountId)
                .userId(userId)
                .operationType(operationType)
                .productCode(productCode)
                .tenure(tenure)
                .amount(amount)
                .price(price)
                .currency(currency)
                .status(InvestmentOperationStatus.DEBIT_REQUESTED)
                .debitReference("INVESTMENT_DEBIT:" + id)
                .compensationReference("INVESTMENT_COMPENSATION:" + id)
                .retryCount(0)
                .nextAttemptAt(now.plusMinutes(1))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public boolean matches(String accountId, String userId, InvestmentOperationType type,
                           String productCode, Integer tenure, BigDecimal amount) {
        return Objects.equals(this.accountId, accountId)
                && Objects.equals(this.userId, userId)
                && this.operationType == type
                && Objects.equals(this.productCode, productCode)
                && Objects.equals(this.tenure, tenure)
                && this.amount != null
                && this.amount.compareTo(amount) == 0;
    }

    public void markDebited() {
        status = InvestmentOperationStatus.DEBITED;
        nextAttemptAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    public void complete(UUID targetId) {
        this.targetId = targetId;
        status = InvestmentOperationStatus.COMPLETED;
        nextAttemptAt = null;
        updatedAt = LocalDateTime.now();
    }

    public void markCompensationPending(String reason) {
        status = InvestmentOperationStatus.COMPENSATION_PENDING;
        failureReason = reason;
        nextAttemptAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    public void markCompensated() {
        status = InvestmentOperationStatus.COMPENSATED;
        nextAttemptAt = null;
        updatedAt = LocalDateTime.now();
    }

    public void scheduleRetry(String reason) {
        retryCount = retryCount == null ? 1 : retryCount + 1;
        failureReason = reason;
        long delaySeconds = Math.min(3600L, 30L * (1L << Math.min(retryCount, 6)));
        nextAttemptAt = LocalDateTime.now().plusSeconds(delaySeconds);
        updatedAt = LocalDateTime.now();
    }
}
