package id.payu.account.domain.model;

/**
 * Period types for Budget entities.
 */
public enum BudgetPeriod {
    /**
     * Budget resets daily at midnight.
     */
    DAILY,

    /**
     * Budget resets weekly.
     */
    WEEKLY,

    /**
     * Budget resets monthly.
     */
    MONTHLY
}
