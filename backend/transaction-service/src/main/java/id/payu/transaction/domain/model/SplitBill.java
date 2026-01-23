package id.payu.transaction.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SplitBill {
    private UUID id;
    private String referenceNumber;
    private UUID creatorAccountId;
    private BigDecimal totalAmount;
    private String currency;
    private String title;
    private String description;
    private SplitType splitType;
    private SplitStatus status;
    private Instant dueDate;
    private List<SplitBillParticipant> participants;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    public enum SplitType {
        EQUAL,
        CUSTOM,
        PERCENTAGE
    }

    public enum SplitStatus {
        DRAFT,
        ACTIVE,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }

    public boolean isFullyPaid() {
        if (participants == null || participants.isEmpty()) {
            return false;
        }
        return participants.stream()
                .allMatch(p -> p.getAmountPaid().compareTo(p.getAmountOwed()) >= 0);
    }

    public BigDecimal getTotalPaid() {
        if (participants == null || participants.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return participants.stream()
                .map(SplitBillParticipant::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getRemainingAmount() {
        return totalAmount.subtract(getTotalPaid());
    }

    public boolean canBeCancelled() {
        return status == SplitStatus.DRAFT || status == SplitStatus.ACTIVE;
    }

    public boolean canAddPayment() {
        return status == SplitStatus.ACTIVE || status == SplitStatus.IN_PROGRESS;
    }
}
