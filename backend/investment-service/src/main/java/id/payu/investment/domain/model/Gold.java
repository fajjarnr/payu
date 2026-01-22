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
public class Gold {

    private UUID id;
    private String userId;
    private BigDecimal amount;
    private BigDecimal averageBuyPrice;
    private BigDecimal currentPrice;
    private BigDecimal currentValue;
    private BigDecimal unrealizedProfitLoss;
    private LocalDateTime lastPriceUpdate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
