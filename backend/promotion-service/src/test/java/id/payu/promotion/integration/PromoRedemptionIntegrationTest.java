package id.payu.promotion.integration;

import id.payu.promotion.application.service.PromoRedemptionService;
import id.payu.promotion.domain.model.*;
import id.payu.promotion.domain.port.out.PromoCodeRepositoryPort;
import id.payu.promotion.dto.ApplyPromoRequest;
import id.payu.promotion.dto.ApplyPromoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Promo Redemption feature.
 * Tests the complete flow from controller through service to repository.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Promo Redemption Integration Tests")
class PromoRedemptionIntegrationTest {

    @Autowired
    private PromoRedemptionService promoRedemptionService;

    @Autowired
    private PromoCodeRepositoryPort promoCodeRepository;

    private static final String USER_ID = "user-123";
    private static final String PARTNER_ID = "partner-456";
    private static final String TRANSACTION_ID = "txn-789";

    @BeforeEach
    void setUp() {
        // Setup test data
    }

    @Test
    @DisplayName("should apply percentage discount promo successfully")
    void shouldApplyPercentageDiscountPromo() {
        // Given
        PromoCode promo = PromoCode.builder()
                .code("DISCOUNT20")
                .discountValue(BigDecimal.valueOf(20))
                .discountType(DiscountType.PERCENTAGE)
                .usageType(UsageType.UNLIMITED)
                .status(PromoStatus.ACTIVE)
                .build();
        promoCodeRepository.save(promo);

        ApplyPromoRequest request = new ApplyPromoRequest(
                "DISCOUNT20", USER_ID, TRANSACTION_ID, new BigDecimal("100000"), PARTNER_ID
        );

        // When
        ApplyPromoResponse response = promoRedemptionService.applyPromo(request);

        // Then
        assertTrue(response.success());
        assertEquals(new BigDecimal("20000"), response.discountAmount());
        assertEquals(new BigDecimal("80000"), response.finalAmount());
    }

    @Test
    @DisplayName("should apply fixed discount promo successfully")
    void shouldApplyFixedDiscountPromo() {
        // Given
        PromoCode promo = PromoCode.builder()
                .code("CASH5000")
                .discountValue(BigDecimal.valueOf(5000))
                .discountType(DiscountType.FIXED)
                .usageType(UsageType.UNLIMITED)
                .status(PromoStatus.ACTIVE)
                .build();
        promoCodeRepository.save(promo);

        ApplyPromoRequest request = new ApplyPromoRequest(
                "CASH5000", USER_ID, TRANSACTION_ID, new BigDecimal("100000"), PARTNER_ID
        );

        // When
        ApplyPromoResponse response = promoRedemptionService.applyPromo(request);

        // Then
        assertTrue(response.success());
        assertEquals(new BigDecimal("5000"), response.discountAmount());
        assertEquals(new BigDecimal("95000"), response.finalAmount());
    }

    @Test
    @DisplayName("should reject invalid promo code")
    void shouldRejectInvalidPromoCode() {
        // Given
        ApplyPromoRequest request = new ApplyPromoRequest(
                "INVALID", USER_ID, TRANSACTION_ID, new BigDecimal("100000"), PARTNER_ID
        );

        // When
        ApplyPromoResponse response = promoRedemptionService.applyPromo(request);

        // Then
        assertFalse(response.success());
        assertEquals("PROMO_NOT_FOUND", response.errorCode());
    }

    @Test
    @DisplayName("should reject promo below minimum amount")
    void shouldRejectPromoBelowMinimumAmount() {
        // Given
        PromoCode promo = PromoCode.builder()
                .code("BIGONLY")
                .discountValue(BigDecimal.valueOf(10))
                .discountType(DiscountType.PERCENTAGE)
                .minimumAmount(new BigDecimal("100000"))
                .status(PromoStatus.ACTIVE)
                .build();
        promoCodeRepository.save(promo);

        ApplyPromoRequest request = new ApplyPromoRequest(
                "BIGONLY", USER_ID, TRANSACTION_ID, new BigDecimal("50000"), PARTNER_ID
        );

        // When
        ApplyPromoResponse response = promoRedemptionService.applyPromo(request);

        // Then
        assertFalse(response.success());
        assertEquals("MIN_AMOUNT_NOT_MET", response.errorCode());
    }

    @Test
    @DisplayName("should support idempotency key")
    void shouldSupportIdempotencyKey() {
        // Given
        PromoCode promo = PromoCode.builder()
                .code("IDEMPOTENT")
                .discountValue(BigDecimal.valueOf(10))
                .discountType(DiscountType.PERCENTAGE)
                .status(PromoStatus.ACTIVE)
                .build();
        promoCodeRepository.save(promo);

        String idempotencyKey = "idem-key-123";
        ApplyPromoRequest request1 = new ApplyPromoRequest(
                "IDEMPOTENT", USER_ID, TRANSACTION_ID, new BigDecimal("100000"), PARTNER_ID, idempotencyKey
        );

        // When - First call
        ApplyPromoResponse response1 = promoRedemptionService.applyPromo(request1);

        // When - Second call with same idempotency key
        ApplyPromoRequest request2 = new ApplyPromoRequest(
                "IDEMPOTENT", USER_ID, "different-txn", new BigDecimal("200000"), PARTNER_ID, idempotencyKey
        );
        ApplyPromoResponse response2 = promoRedemptionService.applyPromo(request2);

        // Then - Both responses should be identical (cached result)
        assertTrue(response1.success());
        assertTrue(response2.success());
        assertEquals(response1.discountAmount(), response2.discountAmount());
        assertEquals(response1.finalAmount(), response2.finalAmount());
    }
}
