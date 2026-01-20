package id.payu.wallet.dto;

import java.math.BigDecimal;

public class BalanceResponse {
    private String accountId;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private BigDecimal reservedBalance;
    private String currency;

    public BalanceResponse() {
    }

    public BalanceResponse(String accountId, BigDecimal balance, BigDecimal availableBalance, BigDecimal reservedBalance, String currency) {
        this.accountId = accountId;
        this.balance = balance;
        this.availableBalance = availableBalance;
        this.reservedBalance = reservedBalance;
        this.currency = currency;
    }

    public static BalanceResponseBuilder builder() {
        return new BalanceResponseBuilder();
    }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }
    public BigDecimal getReservedBalance() { return reservedBalance; }
    public void setReservedBalance(BigDecimal reservedBalance) { this.reservedBalance = reservedBalance; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public static class BalanceResponseBuilder {
        private String accountId;
        private BigDecimal balance;
        private BigDecimal availableBalance;
        private BigDecimal reservedBalance;
        private String currency;

        BalanceResponseBuilder() {}

        public BalanceResponseBuilder accountId(String accountId) { this.accountId = accountId; return this; }
        public BalanceResponseBuilder balance(BigDecimal balance) { this.balance = balance; return this; }
        public BalanceResponseBuilder availableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; return this; }
        public BalanceResponseBuilder reservedBalance(BigDecimal reservedBalance) { this.reservedBalance = reservedBalance; return this; }
        public BalanceResponseBuilder currency(String currency) { this.currency = currency; return this; }

        public BalanceResponse build() {
            return new BalanceResponse(accountId, balance, availableBalance, reservedBalance, currency);
        }
    }
}
