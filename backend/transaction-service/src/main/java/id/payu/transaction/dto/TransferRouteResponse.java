package id.payu.transaction.dto;

import id.payu.transaction.domain.model.TransferMethod;
import id.payu.transaction.domain.model.TransferRoute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for transfer route information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRouteResponse {

    private TransferMethod method;
    private String methodDisplay;
    private BigDecimal fee;
    private String currency;
    private String estimatedTime;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private boolean eligible;

    /**
     * Creates a response DTO from a domain entity.
     *
     * @param route the domain entity
     * @param eligible whether this route is eligible for the requested amount
     * @return the response DTO
     */
    public static TransferRouteResponse fromEntity(TransferRoute route, boolean eligible) {
        return TransferRouteResponse.builder()
                .method(route.getMethod())
                .methodDisplay(formatMethodName(route.getMethod()))
                .fee(route.getFee().getAmount())
                .currency(route.getFee().getCurrency().getCurrencyCode())
                .estimatedTime(route.getEstimatedTimeDisplay())
                .minAmount(route.getMinAmount().getAmount())
                .maxAmount(route.getMaxAmount().getAmount())
                .eligible(eligible)
                .build();
    }

    private static String formatMethodName(TransferMethod method) {
        return switch (method) {
            case BI_FAST -> "BI-FAST (Real-time)";
            case RTGS -> "RTGS (High Value)";
            case SKN -> "SKN (Batch)";
        };
    }
}
