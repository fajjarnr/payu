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
public class InvestmentAccount {

    private UUID id;
    private String userId;
    private BigDecimal totalBalance;
    private BigDecimal availableBalance;
    private BigDecimal lockedBalance;
    private AccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum AccountStatus {
        ACTIVE, SUSPENDED, CLOSED, PENDING_VERIFICATION
    }
}
