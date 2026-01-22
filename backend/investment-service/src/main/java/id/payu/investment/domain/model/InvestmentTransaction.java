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
public class InvestmentTransaction {

    private UUID id;
    private String accountId;
    private TransactionType type;
    private InvestmentType investmentType;
    private String investmentId;
    private BigDecimal amount;
    private BigDecimal price;
    private BigDecimal units;
    private BigDecimal fee;
    private String currency;
    private TransactionStatus status;
    private String referenceNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum TransactionType {
        BUY, SELL
    }

    public enum InvestmentType {
        DEPOSIT, MUTUAL_FUND, GOLD
    }

    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, CANCELLED
    }
}
