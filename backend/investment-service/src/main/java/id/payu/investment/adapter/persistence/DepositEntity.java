package id.payu.investment.adapter.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deposits")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "tenure", nullable = false)
    private int tenure;

    @Column(name = "interest_rate", precision = 5, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "maturity_amount", precision = 19, scale = 4)
    private BigDecimal maturityAmount;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "maturity_date")
    private LocalDateTime maturityDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private DepositStatus status = DepositStatus.ACTIVE;

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "IDR";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum DepositStatus {
        ACTIVE, MATURED, WITHDRAWN, CANCELLED
    }
}
