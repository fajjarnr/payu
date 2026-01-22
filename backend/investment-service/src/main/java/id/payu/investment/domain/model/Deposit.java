package id.payu.investment.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deposit {

    private UUID id;
    private String accountId;
    private BigDecimal amount;
    private int tenure;
    private BigDecimal interestRate;
    private BigDecimal maturityAmount;
    private LocalDateTime startDate;
    private LocalDateTime maturityDate;
    private DepositStatus status;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum DepositStatus {
        ACTIVE, MATURED, WITHDRAWN, CANCELLED
    }
}
