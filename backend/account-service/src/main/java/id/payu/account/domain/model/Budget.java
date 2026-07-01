package id.payu.account.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Budget domain model for spending limits and budget management.
 *
 * <p>This entity represents a user's budget for a specific spending category,
 * with support for daily, weekly, and monthly periods.</p>
 *
 * <p>Domain behaviors:</p>
 * <ul>
 *   <li>Track spending against a defined limit</li>
 *   <li>Automatic period reset when needed</li>
 *   <li>Status calculation (ACTIVE, NEAR_LIMIT, EXCEEDED, PAUSED)</li>
 *   <li>Validation for spending requests</li>
 * </ul>
 */
public class Budget {

    private UUID id;
    private UUID userId;
    private String category;
    private BigDecimal limitAmount;
    private BudgetPeriod period;
    private BigDecimal currentSpent;
    private LocalDate resetDate;
    private boolean active;
    private BigDecimal warningThreshold; // Percentage (e.g., 0.8 for 80%)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long version;

    // Default warning threshold at 80%
    private static final BigDecimal DEFAULT_WARNING_THRESHOLD = new BigDecimal("0.8");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public Budget() {
        this.active = true;
        this.currentSpent = BigDecimal.ZERO;
        this.warningThreshold = DEFAULT_WARNING_THRESHOLD;
    }

    public Budget(UUID id, UUID userId, String category, BigDecimal limitAmount,
                  BudgetPeriod period, BigDecimal currentSpent, LocalDate resetDate,
                  boolean active, LocalDateTime createdAt, LocalDateTime updatedAt, long version) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.limitAmount = limitAmount;
        this.period = period;
        this.currentSpent = currentSpent != null ? currentSpent : BigDecimal.ZERO;
        this.resetDate = resetDate;
        this.active = active;
        this.warningThreshold = DEFAULT_WARNING_THRESHOLD;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    // ==================== DOMAIN BEHAVIORS ====================

    /**
     * Checks if a spending amount is allowed within this budget.
     *
     * Business rules:
     * - Budget must be active
     * - Total spent after transaction must not exceed limit (unless in lenient mode)
     *
     * @param amount the amount to spend
     * @return true if spending is allowed
     */
    public boolean canSpend(BigDecimal amount) {
        if (!active) {
            return false;
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        BigDecimal projectedSpent = currentSpent.add(amount);
        return projectedSpent.compareTo(limitAmount) <= 0;
    }

    /**
     * Records a spending transaction against this budget.
     *
     * @param amount the amount spent
     * @throws IllegalStateException if budget is not active
     * @throws IllegalArgumentException if amount is invalid
     */
    public void recordSpending(BigDecimal amount) {
        if (!active) {
            throw new IllegalStateException("Cannot record spending on inactive budget");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        this.currentSpent = this.currentSpent.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Resets the budget spending if the period has elapsed.
     * Should be called before checking budget status.
     */
    public void resetIfNeeded() {
        if (resetDate == null) {
            // First time setup
            this.resetDate = calculateNextResetDate();
            return;
        }

        LocalDate today = LocalDate.now();
        if (today.isAfter(resetDate) || today.isEqual(resetDate)) {
            // Period has elapsed, reset spending
            this.currentSpent = BigDecimal.ZERO;
            this.resetDate = calculateNextResetDate();
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Calculates the next reset date based on the period type.
     */
    private LocalDate calculateNextResetDate() {
        LocalDate today = LocalDate.now();
        return switch (period) {
            case DAILY -> today.plusDays(1);
            case WEEKLY -> today.plusWeeks(1);
            case MONTHLY -> today.plusMonths(1);
        };
    }

    /**
     * Pauses the budget, preventing new spending from being recorded.
     */
    public void pause() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Resumes the budget, allowing spending to be recorded again.
     */
    public void resume() {
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Updates the budget limit.
     *
     * @param newLimit the new limit amount
     */
    public void updateLimit(BigDecimal newLimit) {
        if (newLimit == null || newLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        this.limitAmount = newLimit;
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== QUERY METHODS ====================

    /**
     * Gets the current budget status based on spending.
     *
     * @return BudgetStatus indicating current state
     */
    public BudgetStatus getStatus() {
        if (!active) {
            return BudgetStatus.PAUSED;
        }

        BigDecimal percentage = getSpentPercentage();

        if (percentage.compareTo(ONE_HUNDRED) >= 0) {
            return BudgetStatus.EXCEEDED;
        }

        if (percentage.compareTo(warningThreshold.multiply(ONE_HUNDRED)) >= 0) {
            return BudgetStatus.NEAR_LIMIT;
        }

        return BudgetStatus.ACTIVE;
    }

    /**
     * Calculates the percentage of budget spent.
     *
     * @return percentage spent (0-100+)
     */
    public BigDecimal getSpentPercentage() {
        if (limitAmount == null || limitAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentSpent.multiply(ONE_HUNDRED).divide(limitAmount, 2, java.math.RoundingMode.HALF_EVEN);
    }

    /**
     * Gets the remaining budget amount.
     *
     * @return remaining amount (can be negative if exceeded)
     */
    public BigDecimal getRemainingAmount() {
        return limitAmount.subtract(currentSpent);
    }

    /**
     * Checks if the budget has been exceeded.
     *
     * @return true if spending exceeds limit
     */
    public boolean isExceeded() {
        return currentSpent.compareTo(limitAmount) > 0;
    }

    /**
     * Checks if the budget is near its limit (above warning threshold).
     *
     * @return true if near limit
     */
    public boolean isNearLimit() {
        BigDecimal threshold = limitAmount.multiply(warningThreshold);
        return currentSpent.compareTo(threshold) >= 0 && !isExceeded();
    }

    // ==================== GETTERS AND SETTERS ====================

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
    }

    public BudgetPeriod getPeriod() {
        return period;
    }

    public void setPeriod(BudgetPeriod period) {
        this.period = period;
    }

    public BigDecimal getCurrentSpent() {
        return currentSpent;
    }

    public void setCurrentSpent(BigDecimal currentSpent) {
        this.currentSpent = currentSpent;
    }

    public LocalDate getResetDate() {
        return resetDate;
    }

    public void setResetDate(LocalDate resetDate) {
        this.resetDate = resetDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public BigDecimal getWarningThreshold() {
        return warningThreshold;
    }

    public void setWarningThreshold(BigDecimal warningThreshold) {
        this.warningThreshold = warningThreshold;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    // ==================== BUILDER ====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID userId;
        private String category;
        private BigDecimal limitAmount;
        private BudgetPeriod period;
        private BigDecimal currentSpent;
        private LocalDate resetDate;
        private boolean active = true;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private long version;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder limitAmount(BigDecimal limitAmount) {
            this.limitAmount = limitAmount;
            return this;
        }

        public Builder period(BudgetPeriod period) {
            this.period = period;
            return this;
        }

        public Builder currentSpent(BigDecimal currentSpent) {
            this.currentSpent = currentSpent;
            return this;
        }

        public Builder resetDate(LocalDate resetDate) {
            this.resetDate = resetDate;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder version(long version) {
            this.version = version;
            return this;
        }

        public Budget build() {
            return new Budget(id, userId, category, limitAmount, period, currentSpent,
                    resetDate, active, createdAt, updatedAt, version);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Budget budget = (Budget) o;
        return id != null && id.equals(budget.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Budget{" +
                "id=" + id +
                ", userId=" + userId +
                ", category='" + category + '\'' +
                ", limitAmount=" + limitAmount +
                ", period=" + period +
                ", currentSpent=" + currentSpent +
                ", status=" + getStatus() +
                '}';
    }
}
