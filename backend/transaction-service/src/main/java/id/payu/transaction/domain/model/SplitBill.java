package id.payu.transaction.domain.model;

import jakarta.persistence.*;
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
@Entity
@Table(name = "split_bills")
public class SplitBill {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reference_number", nullable = false, unique = true)
    private String referenceNumber;

    @Column(name = "creator_account_id", nullable = false)
    private UUID creatorAccountId;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false)
    private SplitType splitType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SplitStatus status;

    @Column(name = "due_date")
    private Instant dueDate;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "split_bill_id")
    private List<SplitBillParticipant> participants;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "completed_at")
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
