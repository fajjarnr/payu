package id.payu.api.common.money;

import id.payu.api.common.exception.BusinessException;

/**
 * Exception thrown when a monetary operation would result in insufficient funds.
 * <p>
 * This is a business exception that extends {@link BusinessException} to maintain
 * consistent error handling across PayU services.
 *
 * @author PayU Digital Banking Platform
 * @since 1.0.0
 */
public class InsufficientFundsException extends BusinessException {

    private static final String ERROR_CODE = "MONEY_001";

    private final Money available;
    private final Money requested;

    /**
     * Creates an InsufficientFundsException with the specified message.
     *
     * @param message the error message
     */
    public InsufficientFundsException(String message) {
        super(ERROR_CODE, message);
        this.available = null;
        this.requested = null;
    }

    /**
     * Creates an InsufficientFundsException with available and requested amounts.
     *
     * @param available the available amount
     * @param requested the requested amount
     * @param message   the error message
     */
    public InsufficientFundsException(Money available, Money requested, String message) {
        super(ERROR_CODE, message);
        this.available = available;
        this.requested = requested;
    }

    /**
     * Creates an InsufficientFundsException with available and requested amounts and cause.
     *
     * @param available the available amount
     * @param requested the requested amount
     * @param message   the error message
     * @param cause     the cause of this exception
     */
    public InsufficientFundsException(Money available, Money requested, String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
        this.available = available;
        this.requested = requested;
    }

    /**
     * Returns the available amount at the time of the exception.
     *
     * @return the available money, or null if not provided
     */
    public Money getAvailable() {
        return available;
    }

    /**
     * Returns the requested amount that caused the exception.
     *
     * @return the requested money, or null if not provided
     */
    public Money getRequested() {
        return requested;
    }

    /**
     * Returns the shortfall amount (requested - available).
     *
     * @return the shortfall money, or null if amounts not provided
     */
    public Money getShortfall() {
        if (available == null || requested == null) {
            return null;
        }
        // Since subtract would throw another exception, calculate directly
        return Money.of(
                requested.getAmount().subtract(available.getAmount()),
                requested.getCurrencyCode()
        );
    }
}
