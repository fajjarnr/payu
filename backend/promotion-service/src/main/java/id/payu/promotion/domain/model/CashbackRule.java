package id.payu.promotion.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Aggregate Root representing a CashbackEntity Rule in the domain.
 * Rich domain model with behavior methods for evaluating and calculating cashback.
 */
public class CashbackRule {

    private String ruleId;
    private String name;
    private CashbackType cashbackType;
    private BigDecimal cashbackAmount;
    private Double cashbackPercentage;
    private BigDecimal maxCashback;
    private BigDecimal minAmount;
    private BigDecimal exactAmount;
    private Map<BigDecimal, BigDecimal> tieredCashback;
    private Set<String> applicableMerchantCodes;
    private Set<String> applicableCategories;
    private boolean active;
    private Instant validFrom;
    private Instant validUntil;

    public CashbackRule() {
        this.active = true;
        this.applicableMerchantCodes = new HashSet<>();
        this.applicableCategories = new HashSet<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if this rule matches the given transaction.
     *
     * @param transaction the transaction to check
     * @return true if the rule applies to this transaction
     */
    public boolean matches(Transaction transaction) {
        // Check if rule is active
        if (!active) {
            return false;
        }

        // Check validity period
        Instant now = Instant.now();
        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }
        if (validUntil != null && now.isAfter(validUntil)) {
            return false;
        }

        // Check exact amount if specified
        if (exactAmount != null) {
            return transaction.getAmount().compareTo(exactAmount) == 0;
        }

        // Check minimum amount
        if (minAmount != null && transaction.getAmount().compareTo(minAmount) < 0) {
            return false;
        }

        // Check merchant code restrictions
        if (!applicableMerchantCodes.isEmpty() &&
                !applicableMerchantCodes.contains(transaction.getMerchantCode())) {
            return false;
        }

        // Check category restrictions
        if (!applicableCategories.isEmpty() &&
                !applicableCategories.contains(transaction.getCategoryCode())) {
            return false;
        }

        return true;
    }

    /**
     * Calculates the cashback amount for a matching transaction.
     *
     * @param transaction the transaction
     * @return the cashback amount (zero if not matching)
     */
    public BigDecimal calculateCashback(Transaction transaction) {
        if (!matches(transaction)) {
            return BigDecimal.ZERO;
        }

        BigDecimal calculatedCashback;

        switch (cashbackType) {
            case FIXED:
                calculatedCashback = cashbackAmount != null ? cashbackAmount : BigDecimal.ZERO;
                break;

            case PERCENTAGE:
                if (cashbackPercentage != null) {
                     calculatedCashback = transaction.getAmount()
                             .multiply(BigDecimal.valueOf(cashbackPercentage))
                             .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN);
                } else {
                    calculatedCashback = BigDecimal.ZERO;
                }
                break;

            case TIERED:
                calculatedCashback = calculateTieredCashback(transaction.getAmount());
                break;

            default:
                calculatedCashback = BigDecimal.ZERO;
        }

        // Apply max cashback cap if set
        if (maxCashback != null && calculatedCashback.compareTo(maxCashback) > 0) {
            calculatedCashback = maxCashback;
        }

        return calculatedCashback;
    }

    /**
     * Calculates cashback based on tiered thresholds.
     */
    private BigDecimal calculateTieredCashback(BigDecimal amount) {
        if (tieredCashback == null || tieredCashback.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal applicableCashback = BigDecimal.ZERO;
        BigDecimal highestThreshold = BigDecimal.ZERO;

        // Find the highest tier that applies
        for (Map.Entry<BigDecimal, BigDecimal> tier : tieredCashback.entrySet()) {
            BigDecimal tierThreshold = tier.getKey();
            if (amount.compareTo(tierThreshold) >= 0 && tierThreshold.compareTo(highestThreshold) >= 0) {
                highestThreshold = tierThreshold;
                applicableCashback = tier.getValue();
            }
        }

        return applicableCashback;
    }

    // Getters and setters
    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CashbackType getCashbackType() {
        return cashbackType;
    }

    public void setCashbackType(CashbackType cashbackType) {
        this.cashbackType = cashbackType;
    }

    public BigDecimal getCashbackAmount() {
        return cashbackAmount;
    }

    public void setCashbackAmount(BigDecimal cashbackAmount) {
        this.cashbackAmount = cashbackAmount;
    }

    public Double getCashbackPercentage() {
        return cashbackPercentage;
    }

    public void setCashbackPercentage(Double cashbackPercentage) {
        this.cashbackPercentage = cashbackPercentage;
    }

    public BigDecimal getMaxCashback() {
        return maxCashback;
    }

    public void setMaxCashback(BigDecimal maxCashback) {
        this.maxCashback = maxCashback;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getExactAmount() {
        return exactAmount;
    }

    public void setExactAmount(BigDecimal exactAmount) {
        this.exactAmount = exactAmount;
    }

    public Map<BigDecimal, BigDecimal> getTieredCashback() {
        return tieredCashback;
    }

    public void setTieredCashback(Map<BigDecimal, BigDecimal> tieredCashback) {
        this.tieredCashback = tieredCashback;
    }

    public Set<String> getApplicableMerchantCodes() {
        return applicableMerchantCodes;
    }

    public void setApplicableMerchantCodes(Set<String> applicableMerchantCodes) {
        this.applicableMerchantCodes = applicableMerchantCodes;
    }

    public Set<String> getApplicableCategories() {
        return applicableCategories;
    }

    public void setApplicableCategories(Set<String> applicableCategories) {
        this.applicableCategories = applicableCategories;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }

    public static class Builder {
        private final CashbackRule rule = new CashbackRule();

        public Builder ruleId(String ruleId) {
            rule.ruleId = ruleId;
            return this;
        }

        public Builder name(String name) {
            rule.name = name;
            return this;
        }

        public Builder cashbackType(CashbackType cashbackType) {
            rule.cashbackType = cashbackType;
            return this;
        }

        public Builder cashbackAmount(BigDecimal cashbackAmount) {
            rule.cashbackAmount = cashbackAmount;
            return this;
        }

        public Builder cashbackPercentage(double cashbackPercentage) {
            rule.cashbackPercentage = cashbackPercentage;
            return this;
        }

        public Builder maxCashback(BigDecimal maxCashback) {
            rule.maxCashback = maxCashback;
            return this;
        }

        public Builder minAmount(BigDecimal minAmount) {
            rule.minAmount = minAmount;
            return this;
        }

        public Builder exactAmount(BigDecimal exactAmount) {
            rule.exactAmount = exactAmount;
            return this;
        }

        public Builder tieredCashback(Map<BigDecimal, BigDecimal> tieredCashback) {
            rule.tieredCashback = tieredCashback;
            return this;
        }

        public Builder applicableMerchantCodes(Set<String> applicableMerchantCodes) {
            rule.applicableMerchantCodes = applicableMerchantCodes;
            return this;
        }

        public Builder applicableCategories(Set<String> applicableCategories) {
            rule.applicableCategories = applicableCategories;
            return this;
        }

        public Builder active(boolean active) {
            rule.active = active;
            return this;
        }

        public Builder validFrom(Instant validFrom) {
            rule.validFrom = validFrom;
            return this;
        }

        public Builder validUntil(Instant validUntil) {
            rule.validUntil = validUntil;
            return this;
        }

        public CashbackRule build() {
            if (rule.ruleId == null) {
                throw new IllegalArgumentException("ruleId is required");
            }
            return rule;
        }
    }
}
