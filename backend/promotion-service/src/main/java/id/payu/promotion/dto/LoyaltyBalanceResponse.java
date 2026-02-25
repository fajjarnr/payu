package id.payu.promotion.dto;

import java.time.Instant;

/**
 * Response DTO for loyalty balance information.
 * Includes expiring point data required by frontend (XBUG-012 fix).
 */
public record LoyaltyBalanceResponse(
    Integer currentBalance,
    Integer totalEarned,
    Integer totalRedeemed,
    Integer expiredPoints,
    /** Points that will expire by the next expiry date. */
    Integer pointsExpiring,
    /** Next point expiry date (ISO-8601). Null if no points are expiring. */
    Instant expiryDate
) {
    /** Backward-compatible constructor for existing code. */
    public LoyaltyBalanceResponse(Integer currentBalance, Integer totalEarned,
                                  Integer totalRedeemed, Integer expiredPoints) {
        this(currentBalance, totalEarned, totalRedeemed, expiredPoints, 0, null);
    }
}
