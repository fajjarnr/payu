package id.payu.promotion.domain.model;

import id.payu.promotion.domain.exception.InvalidPromoException;
import id.payu.promotion.domain.exception.MinimumAmountNotMetException;
import id.payu.promotion.domain.exception.PromoAlreadyUsedException;
import id.payu.promotion.domain.exception.PromoExpiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Domain tests for PromoCode entity (TDD - RED phase first).
 * Tests the rich domain model behavior for promo code redemption.
 */
@DisplayName("PromoCode Domain Tests")
class PromoCodeTest {

    private static final String USER_ID = "user-123";
    private static final String PARTNER_ID = "partner-456";

        private BigDecimal discount(String value) {
                return new BigDecimal(value);
        }

    private TransactionContext createContext(BigDecimal amount) {
        return TransactionContext.builder()
                .userId(USER_ID)
                .amount(amount)
                .partnerId(PARTNER_ID)
                .transactionId(UUID.randomUUID().toString())
                .build();
    }

    @Test
    @DisplayName("should apply valid promo code with percentage discount")
    void shouldApplyValidPromoCode() {
        // Given
        PromoCode promo = PromoCode.builder()
                .code("DISCOUNT10")
                .discountValue(discount("10"))
                .discountType(DiscountType.PERCENTAGE)
                .build();

        TransactionContext ctx = createContext(new BigDecimal("100.00"));

        // When
        PromoResult result = promo.apply(ctx);

        // Then
        assertTrue(result.isSuccess(), "Promo should be applied successfully");
        assertEquals(new BigDecimal("10.0000"), result.getDiscountAmount(), "Discount should be 10% of 100");
        assertEquals(new BigDecimal("90.0000"), result.getFinalAmount(), "Final amount should be 90 after discount");
        assertEquals("DISCOUNT10", result.getPromoCode());
    }

    @Test
    @DisplayName("should preview a valid promo without consuming it")
    void shouldPreviewWithoutSideEffects() {
        PromoCode promo = PromoCode.builder()
                .code("PREVIEW")
                .discountValue(discount("10"))
                .discountType(DiscountType.PERCENTAGE)
                .usageType(UsageType.ONCE_PER_USER)
                .build();

        PromoResult result = promo.preview(createContext(new BigDecimal("100.00")));

        assertTrue(result.isSuccess());
        assertEquals(0, promo.getCurrentUsageCount());
        assertFalse(promo.hasBeenUsedBy(USER_ID));
    }

    @Test
    @DisplayName("should reject expired promo code")
    void shouldRejectExpiredPromo() {
        // Given
        PromoCode promo = PromoCode.builder()
                .code("EXPIRED")
                .discountValue(discount("10"))
                .discountType(DiscountType.PERCENTAGE)
                .expiryDate(Instant.now().minusSeconds(86400))
                .build();

        TransactionContext ctx = createContext(new BigDecimal("100.00"));

        // When & Then
        PromoExpiredException exception = assertThrows(
                PromoExpiredException.class,
                () -> promo.apply(ctx)
        );
        assertTrue(exception.getMessage().contains("expired"));
    }

    @Test
    @DisplayName("should reject already used promo code for one-time use")
    void shouldRejectAlreadyUsedPromo() {
        // Given
        PromoCode promo = PromoCode.builder()
                .code("ONETIME")
                .discountValue(discount("10"))
                .discountType(DiscountType.PERCENTAGE)
                .usageType(UsageType.ONCE_PER_USER)
                .build();

        promo.markUsedBy(USER_ID);

        TransactionContext ctx = createContext(new BigDecimal("100.00"));

        // When & Then
        PromoAlreadyUsedException exception = assertThrows(
                PromoAlreadyUsedException.class,
                () -> promo.apply(ctx)
        );
        assertTrue(exception.getMessage().contains("already used"));
    }

    @Test
    @DisplayName("should reject transaction below minimum amount")
    void shouldRejectBelowMinimumTransaction() {
        // Given
        PromoCode promo = PromoCode.builder()
                .code("BIGONLY")
                .discountValue(discount("10"))
                .discountType(DiscountType.PERCENTAGE)
                .minimumAmount(new BigDecimal("100000"))
                .build();

        TransactionContext ctx = createContext(new BigDecimal("50000"));

        // When & Then
        MinimumAmountNotMetException exception = assertThrows(
                MinimumAmountNotMetException.class,
                () -> promo.apply(ctx)
        );
        assertTrue(exception.getMessage().contains("minimum"));
    }

    @Test
    @DisplayName("should calculate fixed discount correctly")
    void shouldCalculateFixedDiscount() {
        // Given
        PromoCode promo = PromoCode.builder()
                .code("CASH5000")
                .discountValue(discount("5000"))
                .discountType(DiscountType.FIXED)
                .build();

        TransactionContext ctx = createContext(new BigDecimal("100000"));

        // When
        PromoResult result = promo.apply(ctx);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("5000"), result.getDiscountAmount());
        assertEquals(new BigDecimal("95000"), result.getFinalAmount());
    }

    @Test
    @DisplayName("percentage discount amount must be scale 4 (ADR-0022, PROMO-004)")
    void percentageDiscountShouldBeScaleFour() {
        PromoCode promo = PromoCode.builder()
                .code("HALFOFF")
                .discountValue(discount("33.3333"))
                .discountType(DiscountType.PERCENTAGE)
                .build();

        PromoResult result = promo.apply(createContext(new BigDecimal("9999.9999")));

        assertTrue(result.isSuccess());
        assertEquals(4, result.getDiscountAmount().scale(),
                "percentage discount must keep scale 4 (HALF_EVEN), not truncate to 2");
    }

    @Test
    @DisplayName("should not exceed maximum discount cap for percentage discount")
    void shouldRespectMaxDiscountCap() {        // Given - 50% discount with 10000 max cap
        PromoCode promo = PromoCode.builder()
                .code("BIGDISCOUNT")
                .discountValue(discount("50"))
                .discountType(DiscountType.PERCENTAGE)
                .maxDiscountAmount(new BigDecimal("10000"))
                .build();

        // Transaction of 100000, 50% would be 50000, but cap is 10000
        TransactionContext ctx = createContext(new BigDecimal("100000"));

        // When
        PromoResult result = promo.apply(ctx);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("10000"), result.getDiscountAmount(), "Should be capped at max discount");
        assertEquals(new BigDecimal("90000"), result.getFinalAmount());
    }

    @Test
    @DisplayName("should mark promo as used after successful application")
    void shouldMarkPromoAsUsed() {
        // Given
        PromoCode promo = PromoCode.builder()
                .code("SINGLEUSE")
                .discountValue(discount("10"))
                .discountType(DiscountType.PERCENTAGE)
                .usageType(UsageType.ONCE_PER_USER)
                .build();

        TransactionContext ctx = createContext(new BigDecimal("100.00"));

        // When
        PromoResult result = promo.apply(ctx);

        // Then
        assertTrue(result.isSuccess());
        assertTrue(promo.hasBeenUsedBy(USER_ID), "Promo should be marked as used by user");
    }

    @Test
    @DisplayName("should allow multiple uses for UNLIMITED usage type")
    void shouldAllowMultipleUsesForUnlimited() {
        // Given
        PromoCode promo = PromoCode.builder()
                .code("UNLIMITED")
                .discountValue(discount("5"))
                .discountType(DiscountType.PERCENTAGE)
                .usageType(UsageType.UNLIMITED)
                .build();

        TransactionContext ctx1 = createContext(new BigDecimal("100.00"));
        TransactionContext ctx2 = createContext(new BigDecimal("200.00"));

        // When
        PromoResult result1 = promo.apply(ctx1);
        PromoResult result2 = promo.apply(ctx2);

        // Then
        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
        assertEquals(new BigDecimal("5.0000"), result1.getDiscountAmount());
        assertEquals(new BigDecimal("10.0000"), result2.getDiscountAmount());
    }

    @Test
    @DisplayName("should reject inactive promo code")
    void shouldRejectInactivePromo() {
        // Given
        PromoCode promo = PromoCode.builder()
                .code("INACTIVE")
                .discountValue(discount("10"))
                .discountType(DiscountType.PERCENTAGE)
                .status(PromoStatus.INACTIVE)
                .build();

        TransactionContext ctx = createContext(new BigDecimal("100.00"));

        // When & Then
        InvalidPromoException exception = assertThrows(
                InvalidPromoException.class,
                () -> promo.apply(ctx)
        );
        assertTrue(exception.getMessage().contains("inactive"));
    }

    @Test
    @DisplayName("should reject promo code for excluded partner")
    void shouldRejectExcludedPartner() {
        // Given
        PromoCode promo = PromoCode.builder()
                .code("NOPARTNER")
                .discountValue(discount("10"))
                .discountType(DiscountType.PERCENTAGE)
                .excludedPartnerIds(java.util.Set.of(PARTNER_ID))
                .build();

        TransactionContext ctx = createContext(new BigDecimal("100.00"));

        // When & Then
        InvalidPromoException exception = assertThrows(
                InvalidPromoException.class,
                () -> promo.apply(ctx)
        );
        assertTrue(exception.getMessage().contains("not eligible"));
    }

    @Test
    @DisplayName("should calculate percentage discount with proper rounding")
    void shouldCalculatePercentageWithRounding() {
        // Given - 33.33% discount on 100 should be 33.33
        PromoCode promo = PromoCode.builder()
                .code("WEIRD")
                .discountValue(discount("33.33"))
                .discountType(DiscountType.PERCENTAGE)
                .build();

        TransactionContext ctx = createContext(new BigDecimal("100.00"));

        // When
        PromoResult result = promo.apply(ctx);

        // Then
        assertTrue(result.isSuccess());
        // Should round to 4 decimal places (ADR-0022)
        assertEquals(new BigDecimal("33.3300"), result.getDiscountAmount());
    }

    @Test
    @DisplayName("should not allow fixed discount greater than transaction amount")
    void shouldNotAllowDiscountGreaterThanAmount() {
        // Given
        PromoCode promo = PromoCode.builder()
                .code("TOOBIG")
                .discountValue(discount("1000"))
                .discountType(DiscountType.FIXED)
                .build();

        TransactionContext ctx = createContext(new BigDecimal("500"));

        // When
        PromoResult result = promo.apply(ctx);

        // Then - discount should be capped at transaction amount
        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("500"), result.getDiscountAmount());
        assertEquals(BigDecimal.ZERO, result.getFinalAmount());
    }

    @Test
    @DisplayName("should track usage count correctly")
    void shouldTrackUsageCount() {
        // Given
        PromoCode promo = PromoCode.builder()
                .code("LIMITED")
                .discountValue(discount("10"))
                .discountType(DiscountType.PERCENTAGE)
                .maxUsageCount(3)
                .build();

        assertEquals(0, promo.getCurrentUsageCount());

        // When
        promo.markUsedBy("user-1");
        promo.markUsedBy("user-2");

        // Then
        assertEquals(2, promo.getCurrentUsageCount());
        assertTrue(promo.canBeUsed());

        // When - use up to limit
        promo.markUsedBy("user-3");

        // Then
        assertEquals(3, promo.getCurrentUsageCount());
        assertFalse(promo.canBeUsed(), "Should not be usable after reaching max count");
    }
}
