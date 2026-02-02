package id.payu.api.common.exception;

/**
 * Exception thrown when a transaction cannot be completed due to insufficient funds.
 * Results in HTTP 422 Unprocessable Entity response.
 * Used for wallet balance checks, account balance validation, etc.
 */
public class InsufficientFundsException extends BusinessException {

    private final String accountId;
    private final java.math.BigDecimal requiredAmount;
    private final java.math.BigDecimal availableBalance;

    /**
     * Creates an InsufficientFundsException with account and balance details.
     *
     * @param accountId       The account/wallet ID
     * @param requiredAmount  The amount required for the transaction
     * @param availableBalance The current available balance
     */
    public InsufficientFundsException(String accountId, java.math.BigDecimal requiredAmount, java.math.BigDecimal availableBalance) {
        super("INSUFFICIENT_FUNDS", String.format("Insufficient funds for account %s. Required: %s, Available: %s",
                accountId, requiredAmount, availableBalance));
        this.accountId = accountId;
        this.requiredAmount = requiredAmount;
        this.availableBalance = availableBalance;
    }

    /**
     * Creates an InsufficientFundsException with a custom message.
     *
     * @param code    Unique error code (e.g., "TXN_BAL_001")
     * @param message Human-readable error message
     */
    public InsufficientFundsException(String code, String message) {
        super(code, message);
        this.accountId = null;
        this.requiredAmount = null;
        this.availableBalance = null;
    }

    /**
     * Creates an InsufficientFundsException with code, message, and details.
     *
     * @param code             Unique error code
     * @param message          Human-readable error message
     * @param accountId        The account/wallet ID
     * @param requiredAmount   The amount required for the transaction
     * @param availableBalance The current available balance
     */
    public InsufficientFundsException(String code, String message, String accountId,
                                     java.math.BigDecimal requiredAmount, java.math.BigDecimal availableBalance) {
        super(code, message);
        this.accountId = accountId;
        this.requiredAmount = requiredAmount;
        this.availableBalance = availableBalance;
    }

    public String getAccountId() {
        return accountId;
    }

    public java.math.BigDecimal getRequiredAmount() {
        return requiredAmount;
    }

    public java.math.BigDecimal getAvailableBalance() {
        return availableBalance;
    }
}
