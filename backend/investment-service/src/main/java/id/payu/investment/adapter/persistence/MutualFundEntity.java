package id.payu.investment.adapter.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mutual_funds")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MutualFundEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private FundType type;

    @Column(name = "nav_per_unit", precision = 19, scale = 4)
    private BigDecimal navPerUnit;

    @Column(name = "minimum_investment", precision = 19, scale = 4)
    private BigDecimal minimumInvestment;

    @Column(name = "management_fee", precision = 5, scale = 4)
    private BigDecimal managementFee;

    @Column(name = "redemption_fee", precision = 5, scale = 4)
    private BigDecimal redemptionFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private FundStatus status = FundStatus.ACTIVE;

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

    public enum FundStatus {
        ACTIVE, SUSPENDED, CLOSED
    }

    public enum FundType {
        MONEY_MARKET, FIXED_INCOME, MIXED, EQUITY, INDEX_FUND
    }
}
