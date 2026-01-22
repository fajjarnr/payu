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
public class MutualFund {

    private UUID id;
    private String code;
    private String name;
    private FundType type;
    private BigDecimal navPerUnit;
    private BigDecimal minimumInvestment;
    private BigDecimal managementFee;
    private BigDecimal redemptionFee;
    private FundStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum FundStatus {
        ACTIVE, SUSPENDED, CLOSED
    }

    public enum FundType {
        MONEY_MARKET, FIXED_INCOME, MIXED, EQUITY, INDEX_FUND
    }
}
