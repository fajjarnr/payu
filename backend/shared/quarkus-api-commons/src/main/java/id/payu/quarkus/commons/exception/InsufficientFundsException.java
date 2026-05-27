package id.payu.quarkus.commons.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends BusinessException {

    private final String accountId;
    private final BigDecimal requiredAmount;
    private final BigDecimal availableBalance;

    public InsufficientFundsException(String accountId, BigDecimal requiredAmount, BigDecimal availableBalance) {
        super("INSUFFICIENT_FUNDS", String.format("Insufficient funds for account %s. Required: %s, Available: %s",
                accountId, requiredAmount, availableBalance));
        this.accountId = accountId;
        this.requiredAmount = requiredAmount;
        this.availableBalance = availableBalance;
    }

    public InsufficientFundsException(String code, String message) {
        super(code, message);
        this.accountId = null;
        this.requiredAmount = null;
        this.availableBalance = null;
    }

    public InsufficientFundsException(String code, String message, String accountId,
                                      BigDecimal requiredAmount, BigDecimal availableBalance) {
        super(code, message);
        this.accountId = accountId;
        this.requiredAmount = requiredAmount;
        this.availableBalance = availableBalance;
    }

    public String getAccountId() { return accountId; }
    public BigDecimal getRequiredAmount() { return requiredAmount; }
    public BigDecimal getAvailableBalance() { return availableBalance; }
}
