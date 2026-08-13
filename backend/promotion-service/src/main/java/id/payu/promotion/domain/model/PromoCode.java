package id.payu.promotion.domain.model;

import id.payu.promotion.domain.exception.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Aggregate Root representing a Promo Code in the domain.
 * Rich domain model with behavior methods for applying promo codes.
 */
public class PromoCode {

    private String code;
    private BigDecimal discountValue = BigDecimal.ZERO;
    private DiscountType discountType;
    private UsageType usageType;
    private PromoStatus status;
    private BigDecimal minimumAmount;
    private BigDecimal maxDiscountAmount;
    private Integer maxUsageCount;
    private int currentUsageCount;
    private Instant expiryDate;
    private Set<String> usedByUserIds;
    private Set<String> excludedPartnerIds;

    public PromoCode() {
        this.usageType = UsageType.UNLIMITED;
        this.status = PromoStatus.ACTIVE;
        this.usedByUserIds = new HashSet<>();
        this.excludedPartnerIds = new HashSet<>();
        this.currentUsageCount = 0;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Applies this promo code to a transaction context.
     *
     * @param context the transaction context
     * @return PromoResult containing discount details
     * @throws PromoExpiredException if promo has expired
     * @throws PromoAlreadyUsedException if user already used this promo (for ONCE_PER_USER)
     * @throws MinimumAmountNotMetException if transaction amount is below minimum
     * @throws InvalidPromoException if promo is inactive or user not eligible
     */
    public PromoResult apply(TransactionContext context) {
        PromoResult result = preview(context);

        // Mark as used
        markUsedBy(context.getUserId());

        return result;
    }

    /**
     * Calculates the result without mutating usage state.
     */
    public PromoResult preview(TransactionContext context) {
        validateCanApply(context);

        BigDecimal originalAmount = context.getAmount();
        BigDecimal discountAmount = calculateDiscount(originalAmount);
        BigDecimal finalAmount = originalAmount.subtract(discountAmount);
        return PromoResult.success(code, originalAmount, discountAmount, finalAmount);
    }

    /**
     * Validates if this promo can be applied to the given context.
     */
    private void validateCanApply(TransactionContext context) {
        // Check status
        if (status != PromoStatus.ACTIVE) {
            throw new InvalidPromoException(code, "Promo is inactive");
        }

        // Check expiry
        if (expiryDate != null && Instant.now().isAfter(expiryDate)) {
            throw new PromoExpiredException(code);
        }

        // Check usage limit
        if (maxUsageCount != null && currentUsageCount >= maxUsageCount) {
            throw new InvalidPromoException(code, "Promo usage limit reached");
        }

        // Check user eligibility
        if (usageType == UsageType.ONCE_PER_USER && usedByUserIds.contains(context.getUserId())) {
            throw new PromoAlreadyUsedException(code, context.getUserId());
        }

        // Check minimum amount
        if (minimumAmount != null && context.getAmount().compareTo(minimumAmount) < 0) {
            throw new MinimumAmountNotMetException(code, minimumAmount, context.getAmount());
        }

        // Check excluded partners
        if (excludedPartnerIds != null && excludedPartnerIds.contains(context.getPartnerId())) {
            throw new InvalidPromoException(code, "User not eligible for this promo");
        }
    }

    /**
     * Calculates the discount amount based on discount type.
     */
    private BigDecimal calculateDiscount(BigDecimal amount) {
        BigDecimal discount;

        if (discountType == DiscountType.PERCENTAGE) {
            discount = amount.multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_EVEN);
        } else {
            discount = discountValue;
        }

        // Apply max discount cap if set
        if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
            discount = maxDiscountAmount;
        }

        // Don't allow discount greater than transaction amount
        if (discount.compareTo(amount) > 0) {
            discount = amount;
        }

        return discount;
    }

    /**
     * Marks this promo as used by a specific user.
     */
    public void markUsedBy(String userId) {
        usedByUserIds.add(userId);
        currentUsageCount++;
    }

    /**
     * Checks if this promo has been used by a specific user.
     */
    public boolean hasBeenUsedBy(String userId) {
        return usedByUserIds.contains(userId);
    }

    /**
     * Checks if this promo can still be used.
     */
    public boolean canBeUsed() {
        if (status != PromoStatus.ACTIVE) {
            return false;
        }
        if (expiryDate != null && Instant.now().isAfter(expiryDate)) {
            return false;
        }
        if (maxUsageCount != null && currentUsageCount >= maxUsageCount) {
            return false;
        }
        return true;
    }

    // Getters and setters for JPA compatibility
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public UsageType getUsageType() {
        return usageType;
    }

    public void setUsageType(UsageType usageType) {
        this.usageType = usageType;
    }

    public PromoStatus getStatus() {
        return status;
    }

    public void setStatus(PromoStatus status) {
        this.status = status;
    }

    public BigDecimal getMinimumAmount() {
        return minimumAmount;
    }

    public void setMinimumAmount(BigDecimal minimumAmount) {
        this.minimumAmount = minimumAmount;
    }

    public BigDecimal getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public Integer getMaxUsageCount() {
        return maxUsageCount;
    }

    public void setMaxUsageCount(Integer maxUsageCount) {
        this.maxUsageCount = maxUsageCount;
    }

    public int getCurrentUsageCount() {
        return currentUsageCount;
    }

    public void setCurrentUsageCount(int currentUsageCount) {
        this.currentUsageCount = currentUsageCount;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Set<String> getUsedByUserIds() {
        return usedByUserIds;
    }

    public void setUsedByUserIds(Set<String> usedByUserIds) {
        this.usedByUserIds = usedByUserIds;
    }

    public Set<String> getExcludedPartnerIds() {
        return excludedPartnerIds;
    }

    public void setExcludedPartnerIds(Set<String> excludedPartnerIds) {
        this.excludedPartnerIds = excludedPartnerIds;
    }

    public static class Builder {
        private final PromoCode promoCode = new PromoCode();

        public Builder code(String code) {
            promoCode.code = code;
            return this;
        }

        public Builder discountValue(BigDecimal discountValue) {
            promoCode.discountValue = discountValue;
            return this;
        }

        public Builder discountType(DiscountType discountType) {
            promoCode.discountType = discountType;
            return this;
        }

        public Builder usageType(UsageType usageType) {
            promoCode.usageType = usageType;
            return this;
        }

        public Builder status(PromoStatus status) {
            promoCode.status = status;
            return this;
        }

        public Builder minimumAmount(BigDecimal minimumAmount) {
            promoCode.minimumAmount = minimumAmount;
            return this;
        }

        public Builder maxDiscountAmount(BigDecimal maxDiscountAmount) {
            promoCode.maxDiscountAmount = maxDiscountAmount;
            return this;
        }

        public Builder maxUsageCount(Integer maxUsageCount) {
            promoCode.maxUsageCount = maxUsageCount;
            return this;
        }

        public Builder expiryDate(Instant expiryDate) {
            promoCode.expiryDate = expiryDate;
            return this;
        }

        public Builder excludedPartnerIds(Set<String> excludedPartnerIds) {
            promoCode.excludedPartnerIds = excludedPartnerIds;
            return this;
        }

        public PromoCode build() {
            if (promoCode.code == null || promoCode.discountType == null) {
                throw new IllegalArgumentException("code and discountType are required");
            }
            return promoCode;
        }
    }
}
