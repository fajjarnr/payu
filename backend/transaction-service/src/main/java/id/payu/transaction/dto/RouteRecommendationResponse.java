package id.payu.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for route recommendation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteRecommendationResponse {

    private TransferRouteResponse route;
    private String reason;
    private BigDecimal totalCost;
    private String currency;
    private String estimatedTime;
}
