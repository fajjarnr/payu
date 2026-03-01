package id.payu.account.domain.model;

/**
 * Status values for Budget entities.
 */
public enum BudgetStatus {
    /**
     * Budget is active and spending is well within limits.
     */
    ACTIVE,

    /**
     * Budget spending has reached the warning threshold (80% by default).
     */
    NEAR_LIMIT,

    /**
     * Budget spending has exceeded the defined limit.
     */
    EXCEEDED,

    /**
     * Budget is paused and not tracking new spending.
     */
    PAUSED
}
