package id.payu.transaction.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SplitBillParticipant {
    private UUID id;
    private UUID splitBillId;
    private UUID accountId;
    private String accountNumber;
    private String accountName;
    private BigDecimal amountOwed;
    private BigDecimal amountPaid;
    private ParticipantStatus status;
    private Instant settledAt;
    private Instant createdAt;
    private Instant updatedAt;

    public enum ParticipantStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        PARTIALLY_PAID,
        SETTLED
    }

    public BigDecimal getRemainingAmount() {
        return amountOwed.subtract(amountPaid);
    }

    public boolean isFullyPaid() {
        return amountPaid.compareTo(amountOwed) >= 0;
    }

    public boolean canMakePayment() {
        return status == ParticipantStatus.ACCEPTED || status == ParticipantStatus.PARTIALLY_PAID;
    }
}
