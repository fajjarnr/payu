package id.payu.partner.domain.port.in;

import id.payu.partner.domain.RateCard;
import id.payu.partner.domain.FeeCalculationResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for rate card operations (GAP-004).
 * Defines use cases for partner pricing configuration.
 */
public interface RateCardUseCase {

    /**
     * Create a new flat fee rate card.
     */
    RateCard createFlatFeeCard(String name, String description,
                                BigDecimal flatFee, String currency, String createdBy);

    /**
     * Create a new percentage fee rate card.
     */
    RateCard createPercentageFeeCard(String name, String description,
                                      BigDecimal percentageFee, String currency,
                                      BigDecimal minimumFee, BigDecimal maximumFee,
                                      String createdBy);

    /**
     * Create a new tiered fee rate card.
     */
    RateCard createTieredFeeCard(String name, String description,
                                  String currency, String createdBy);

    /**
     * Get rate card by ID.
     */
    RateCard getRateCard(UUID cardId);

    /**
     * Get all rate cards.
     */
    List<RateCard> getAllRateCards();

    /**
     * Get active rate cards.
     */
    List<RateCard> getActiveRateCards();

    /**
     * Add fee tier to tiered rate card.
     */
    RateCard addFeeTier(UUID cardId, BigDecimal minAmount, BigDecimal maxAmount,
                        BigDecimal flatFee, BigDecimal percentageFee);

    /**
     * Deactivate a rate card.
     */
    RateCard deactivateRateCard(UUID cardId);

    /**
     * Calculate fee for a given amount using a rate card.
     */
    FeeCalculationResult calculateFee(UUID cardId, BigDecimal amount);

    /**
     * Link rate card to partner.
     */
    void linkRateCardToPartner(UUID cardId, Long partnerId);

    /**
     * Get rate card for partner.
     */
    Optional<RateCard> getRateCardForPartner(Long partnerId);

    /**
     * Calculate fee for partner transaction.
     */
    FeeCalculationResult calculateFeeForPartner(Long partnerId, BigDecimal amount);
}
