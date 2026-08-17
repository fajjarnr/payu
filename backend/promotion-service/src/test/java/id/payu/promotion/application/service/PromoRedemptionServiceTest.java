package id.payu.promotion.application.service;

import id.payu.promotion.domain.model.*;
import id.payu.promotion.domain.port.out.PromoCodeRepositoryPort;
import id.payu.promotion.domain.port.out.PromoUsageRepositoryPort;
import id.payu.promotion.domain.exception.*;
import id.payu.promotion.interfaces.dto.ApplyPromoRequest;
import id.payu.promotion.interfaces.dto.ApplyPromoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Service tests for PromoRedemptionService (TDD - RED phase).
 * Tests the application service orchestrating promo code redemption.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PromoRedemptionService Tests")
class PromoRedemptionServiceTest {

    @Mock
    private PromoCodeRepositoryPort promoCodeRepository;

    @Mock
    private PromoUsageRepositoryPort promoUsageRepository;

    @InjectMocks
    private PromoRedemptionService promoRedemptionService;

    private static final String USER_ID = "user-123";
    private static final String PARTNER_ID = "partner-456";
    private static final String TRANSACTION_ID = "txn-789";

    @BeforeEach
    void setUp() {
        // MockitoExtension handles initialization
    }

    @Test
    @DisplayName("should apply promo and record usage successfully")
    void shouldApplyPromoAndRecordUsage() {
        // Given
        String promoCode = "DISCOUNT10";
        BigDecimal transactionAmount = new BigDecimal("100000");

        ApplyPromoRequest request = new ApplyPromoRequest(
                promoCode, USER_ID, TRANSACTION_ID, transactionAmount, PARTNER_ID
        );

        PromoCode promo = createPromoCode(promoCode, 10, DiscountType.PERCENTAGE);

        when(promoCodeRepository.findByCode(promoCode))
                .thenReturn(Optional.of(promo));
        when(promoUsageRepository.recordUsage(any()))
                .thenReturn(true);

        // When
        ApplyPromoResponse response = promoRedemptionService.applyPromo(request);

        // Then
        assertTrue(response.success());
        assertEquals(0, new BigDecimal("10000").compareTo(response.discountAmount()));
        assertEquals(0, new BigDecimal("90000").compareTo(response.finalAmount()));
        assertEquals(promoCode, response.promoCode());

        // Verify usage was recorded
        ArgumentCaptor<PromoUsage> usageCaptor = ArgumentCaptor.forClass(PromoUsage.class);
        verify(promoUsageRepository).recordUsage(usageCaptor.capture());

        PromoUsage capturedUsage = usageCaptor.getValue();
        assertEquals(USER_ID, capturedUsage.getUserId());
        assertEquals(promoCode, capturedUsage.getPromoCode());
        assertEquals(TRANSACTION_ID, capturedUsage.getTransactionId());
    }

    @Test
    @DisplayName("should validate promo without recording usage or saving promo state")
    void shouldValidatePromoWithoutSideEffects() {
        String promoCode = "PREVIEW10";
        PromoCode promo = createPromoCode(promoCode, 10, DiscountType.PERCENTAGE);
        promo.setUsageType(UsageType.ONCE_PER_USER);
        ApplyPromoRequest request = new ApplyPromoRequest(
                promoCode, USER_ID, "validation", new BigDecimal("100000"), PARTNER_ID
        );
        when(promoCodeRepository.findByCode(promoCode)).thenReturn(Optional.of(promo));
        when(promoUsageRepository.hasUserUsedPromo(USER_ID, promoCode)).thenReturn(false);

        ApplyPromoResponse response = promoRedemptionService.validatePromo(request);

        assertTrue(response.success());
        assertEquals(0, promo.getCurrentUsageCount());
        verify(promoUsageRepository, never()).recordUsage(any());
        verify(promoCodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("should return error for invalid promo code")
    void shouldReturnErrorForInvalidPromo() {
        // Given
        String promoCode = "INVALID";
        ApplyPromoRequest request = new ApplyPromoRequest(
                promoCode, USER_ID, TRANSACTION_ID, new BigDecimal("100000"), PARTNER_ID
        );

        when(promoCodeRepository.findByCode(promoCode))
                .thenReturn(Optional.empty());

        // When
        ApplyPromoResponse response = promoRedemptionService.applyPromo(request);

        // Then
        assertFalse(response.success());
        assertEquals("PROMO_NOT_FOUND", response.errorCode());
        assertNotNull(response.errorMessage());

        // Verify no usage was recorded
        verify(promoUsageRepository, never()).recordUsage(any());
    }

    @Test
    @DisplayName("should rollback usage on transaction failure")
    void shouldRollbackUsageOnTransactionFailure() {
        // Given
        String promoCode = "DISCOUNT10";
        ApplyPromoRequest request = new ApplyPromoRequest(
                promoCode, USER_ID, TRANSACTION_ID, new BigDecimal("100000"), PARTNER_ID
        );

        PromoCode promo = createPromoCode(promoCode, 10, DiscountType.PERCENTAGE);

        when(promoCodeRepository.findByCode(promoCode))
                .thenReturn(Optional.of(promo));
        when(promoUsageRepository.recordUsage(any()))
                .thenThrow(new RuntimeException("Database error"));

        // When
        ApplyPromoResponse response = promoRedemptionService.applyPromo(request);

        // Then
        assertFalse(response.success());
        assertEquals("RECORD_FAILED", response.errorCode());
    }

    @Test
    @DisplayName("should reject already used promo code")
    void shouldRejectAlreadyUsedPromo() {
        // Given
        String promoCode = "ONETIME";
        ApplyPromoRequest request = new ApplyPromoRequest(
                promoCode, USER_ID, TRANSACTION_ID, new BigDecimal("100000"), PARTNER_ID
        );

        PromoCode promo = createPromoCode(promoCode, 10, DiscountType.PERCENTAGE);
        promo.setUsageType(UsageType.ONCE_PER_USER);

        when(promoCodeRepository.findByCode(promoCode))
                .thenReturn(Optional.of(promo));
        when(promoUsageRepository.hasUserUsedPromo(USER_ID, promoCode))
                .thenReturn(true);

        // When
        ApplyPromoResponse response = promoRedemptionService.applyPromo(request);

        // Then
        assertFalse(response.success());
        assertEquals("ALREADY_USED", response.errorCode());
    }

    @Test
    @DisplayName("should reject expired promo code")
    void shouldRejectExpiredPromo() {
        // Given
        String promoCode = "EXPIRED";
        ApplyPromoRequest request = new ApplyPromoRequest(
                promoCode, USER_ID, TRANSACTION_ID, new BigDecimal("100000"), PARTNER_ID
        );

        PromoCode promo = createPromoCode(promoCode, 10, DiscountType.PERCENTAGE);
        promo.setExpiryDate(Instant.now().minusSeconds(86400));

        when(promoCodeRepository.findByCode(promoCode))
                .thenReturn(Optional.of(promo));

        // When
        ApplyPromoResponse response = promoRedemptionService.applyPromo(request);

        // Then
        assertFalse(response.success());
        assertEquals("EXPIRED", response.errorCode());
    }

    @Test
    @DisplayName("should reject transaction below minimum amount")
    void shouldRejectBelowMinimumAmount() {
        // Given
        String promoCode = "BIGONLY";
        ApplyPromoRequest request = new ApplyPromoRequest(
                promoCode, USER_ID, TRANSACTION_ID, new BigDecimal("50000"), PARTNER_ID
        );

        PromoCode promo = createPromoCode(promoCode, 10, DiscountType.PERCENTAGE);
        promo.setMinimumAmount(new BigDecimal("100000"));

        when(promoCodeRepository.findByCode(promoCode))
                .thenReturn(Optional.of(promo));

        // When
        ApplyPromoResponse response = promoRedemptionService.applyPromo(request);

        // Then
        assertFalse(response.success());
        assertEquals("MIN_AMOUNT_NOT_MET", response.errorCode());
    }

    @Test
    @DisplayName("should calculate fixed discount correctly")
    void shouldCalculateFixedDiscount() {
        // Given
        String promoCode = "CASH5000";
        BigDecimal transactionAmount = new BigDecimal("100000");
        ApplyPromoRequest request = new ApplyPromoRequest(
                promoCode, USER_ID, TRANSACTION_ID, transactionAmount, PARTNER_ID
        );

        PromoCode promo = createPromoCode(promoCode, 5000, DiscountType.FIXED);

        when(promoCodeRepository.findByCode(promoCode))
                .thenReturn(Optional.of(promo));
        when(promoUsageRepository.recordUsage(any()))
                .thenReturn(true);

        // When
        ApplyPromoResponse response = promoRedemptionService.applyPromo(request);

        // Then
        assertTrue(response.success());
        assertEquals(0, new BigDecimal("5000").compareTo(response.discountAmount()));
        assertEquals(0, new BigDecimal("95000").compareTo(response.finalAmount()));
    }

    @Test
    @DisplayName("should validate promo with idempotency key")
    void shouldValidatePromoWithIdempotency() {
        // Given
        String promoCode = "DISCOUNT10";
        String idempotencyKey = "idem-123";
        ApplyPromoRequest request = new ApplyPromoRequest(
                promoCode, USER_ID, TRANSACTION_ID, new BigDecimal("100000"), PARTNER_ID, idempotencyKey
        );

        // Simulate already processed with same idempotency key
        when(promoUsageRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(createExistingUsage(promoCode)));

        // When
        ApplyPromoResponse response = promoRedemptionService.applyPromo(request);

        // Then - should return cached result
        assertTrue(response.success());
        assertEquals(new BigDecimal("10000"), response.discountAmount());

        // Should not query promo code or record new usage
        verify(promoCodeRepository, never()).findByCode(any());
        verify(promoUsageRepository, never()).recordUsage(any());
    }

    private PromoCode createPromoCode(String code, double discountValue, DiscountType type) {
        return PromoCode.builder()
                .code(code)
                                .discountValue(BigDecimal.valueOf(discountValue))
                .discountType(type)
                .usageType(UsageType.UNLIMITED)
                .status(PromoStatus.ACTIVE)
                .build();
    }

    private PromoUsage createExistingUsage(String promoCode) {
        PromoUsage usage = new PromoUsage();
        usage.setId(UUID.randomUUID().toString());
        usage.setUserId(USER_ID);
        usage.setPromoCode(promoCode);
        usage.setTransactionId(TRANSACTION_ID);
        usage.setDiscountAmount(new BigDecimal("10000"));
        usage.setFinalAmount(new BigDecimal("90000"));
        usage.setTimestamp(Instant.now());
        return usage;
    }
}
