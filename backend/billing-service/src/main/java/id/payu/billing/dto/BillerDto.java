package id.payu.billing.dto;

import id.payu.billing.domain.BillerType;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for biller information.
 */
public record BillerDto(
    String code,
    String displayName,
    String category,
    BigDecimal adminFee,
    List<String> denominations // For pulsa
) {
    public static BillerDto from(BillerType type) {
        BigDecimal fee = switch (type.getCategory()) {
            case "electricity" -> new BigDecimal("2500");
            case "water" -> new BigDecimal("2000");
            case "mobile" -> BigDecimal.ZERO;
            case "internet" -> new BigDecimal("2500");
            case "insurance" -> new BigDecimal("2500");
            case "utility" -> new BigDecimal("2500");
            case "tv_cable" -> new BigDecimal("2500");
            case "multifinance" -> new BigDecimal("5000");
            case "ewallet" -> new BigDecimal("1000");
            default -> new BigDecimal("2500");
        };

        List<String> denoms = type.getCategory().equals("mobile") 
            ? List.of("5000", "10000", "20000", "25000", "50000", "100000")
            : null;

        return new BillerDto(
            type.getCode(),
            type.getDisplayName(),
            type.getCategory(),
            fee,
            denoms
        );
    }
}
