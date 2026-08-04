package id.payu.billing.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class BillPayment {
    private UUID id;
    private String accountId;
    private String referenceNumber;
    private String idempotencyKey;
    private BillerType billerType;
    private String customerId;
    private BigDecimal amount;
    private BigDecimal adminFee;
    private BigDecimal totalAmount;
    private PaymentStatus status;
    private String failureReason;
    private String billerTransactionId;
    private String walletReservationId;
    private boolean eventPublished;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private String tenantId;
    private Long version;
}
