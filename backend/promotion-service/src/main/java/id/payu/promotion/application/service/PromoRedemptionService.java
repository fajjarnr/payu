package id.payu.promotion.application.service;

import id.payu.promotion.domain.model.*;
import id.payu.promotion.domain.port.out.PromoCodeRepositoryPort;
import id.payu.promotion.domain.port.out.PromoUsageRepositoryPort;
import id.payu.promotion.domain.exception.*;
import id.payu.promotion.dto.ApplyPromoRequest;
import id.payu.promotion.dto.ApplyPromoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Application service for promo code redemption.
 * Orchestrates the domain logic for applying promo codes to transactions.
 */
@Service
public class PromoRedemptionService {

    private static final Logger LOG = LoggerFactory.getLogger(PromoRedemptionService.class);

    private final PromoCodeRepositoryPort promoCodeRepository;
    private final PromoUsageRepositoryPort promoUsageRepository;

    public PromoRedemptionService(
            PromoCodeRepositoryPort promoCodeRepository,
            PromoUsageRepositoryPort promoUsageRepository) {
        this.promoCodeRepository = promoCodeRepository;
        this.promoUsageRepository = promoUsageRepository;
    }

    /**
     * Applies a promo code to a transaction.
     * Implements idempotency check and records usage atomically.
     *
     * @param request the apply promo request
     * @return the response with discount details or error
     */
    @Transactional
    public ApplyPromoResponse applyPromo(ApplyPromoRequest request) {
        LOG.info("Applying promo code: code={}, userId={}, transactionId={}",
                request.promoCode(), request.userId(), request.transactionId());

        // Check idempotency
        if (request.idempotencyKey() != null) {
            Optional<PromoUsage> existingUsage = promoUsageRepository.findByIdempotencyKey(request.idempotencyKey());
            if (existingUsage.isPresent()) {
                LOG.info("Returning cached result for idempotency key: {}", request.idempotencyKey());
                PromoUsage usage = existingUsage.get();
                return ApplyPromoResponse.success(
                        usage.getPromoCode(),
                        usage.getFinalAmount().add(usage.getDiscountAmount()),
                        usage.getDiscountAmount(),
                        usage.getFinalAmount()
                );
            }
        }

        // Find promo code
        Optional<PromoCode> promoOpt = promoCodeRepository.findByCode(request.promoCode());
        if (promoOpt.isEmpty()) {
            LOG.warn("Promo code not found: {}", request.promoCode());
            return ApplyPromoResponse.failure("PROMO_NOT_FOUND", "Invalid promo code");
        }

        PromoCode promo = promoOpt.get();

        // Check if user already used this promo (for ONCE_PER_USER)
        if (promo.getUsageType() == UsageType.ONCE_PER_USER) {
            boolean alreadyUsed = promoUsageRepository.hasUserUsedPromo(request.userId(), request.promoCode());
            if (alreadyUsed) {
                LOG.warn("Promo already used by user: code={}, userId={}", request.promoCode(), request.userId());
                return ApplyPromoResponse.failure("ALREADY_USED", "Promo code has already been used");
            }
        }

        // Build transaction context
        TransactionContext context = TransactionContext.builder()
                .userId(request.userId())
                .amount(request.transactionAmount())
                .partnerId(request.partnerId())
                .transactionId(request.transactionId())
                .build();

        // Apply promo
        PromoResult result;
        try {
            result = promo.apply(context);
        } catch (PromoExpiredException e) {
            LOG.warn("Promo expired: {}", request.promoCode());
            return ApplyPromoResponse.failure("EXPIRED", "Promo code has expired");
        } catch (PromoAlreadyUsedException e) {
            LOG.warn("Promo already used: {}", request.promoCode());
            return ApplyPromoResponse.failure("ALREADY_USED", "Promo code has already been used");
        } catch (MinimumAmountNotMetException e) {
            LOG.warn("Minimum amount not met for promo: {}", request.promoCode());
            return ApplyPromoResponse.failure("MIN_AMOUNT_NOT_MET", e.getMessage());
        } catch (InvalidPromoException e) {
            LOG.warn("Invalid promo: {}", e.getMessage());
            return ApplyPromoResponse.failure("INVALID_PROMO", e.getMessage());
        }

        if (!result.isSuccess()) {
            return ApplyPromoResponse.failure("APPLY_FAILED", "Failed to apply promo code");
        }

        // Record usage
        PromoUsage usage = new PromoUsage();
        usage.setId(UUID.randomUUID().toString());
        usage.setUserId(request.userId());
        usage.setPromoCode(request.promoCode());
        usage.setTransactionId(request.transactionId());
        usage.setDiscountAmount(result.getDiscountAmount());
        usage.setFinalAmount(result.getFinalAmount());
        usage.setIdempotencyKey(request.idempotencyKey());

        try {
            boolean recorded = promoUsageRepository.recordUsage(usage);
            if (!recorded) {
                LOG.error("Failed to record promo usage");
                return ApplyPromoResponse.failure("RECORD_FAILED", "Failed to record promo usage");
            }
        } catch (Exception e) {
            LOG.error("Exception recording promo usage: {}", e.getMessage(), e);
            return ApplyPromoResponse.failure("RECORD_FAILED", "Failed to record promo usage");
        }

        LOG.info("Promo applied successfully: code={}, discount={}, finalAmount={}",
                request.promoCode(), result.getDiscountAmount(), result.getFinalAmount());

        return ApplyPromoResponse.success(
                request.promoCode(),
                result.getOriginalAmount(),
                result.getDiscountAmount(),
                result.getFinalAmount()
        );
    }
}
