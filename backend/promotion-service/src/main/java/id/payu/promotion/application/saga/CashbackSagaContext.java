package id.payu.promotion.application.saga;

import id.payu.promotion.domain.Cashback;
import id.payu.promotion.dto.CreateCashbackRequest;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Context object for cashback saga orchestration.
 * Holds all data needed during saga execution.
 */
public class CashbackSagaContext {

    private CreateCashbackRequest request;
    private Cashback cashback;
    private UUID cashbackId;
    private String accountId;
    private BigDecimal amount;
    private String transactionId;
    private boolean walletCredited;
    private boolean cashbackRecorded;
    private String errorMessage;

    public CashbackSagaContext() {
    }

    public CashbackSagaContext(CreateCashbackRequest request) {
        this.request = request;
        this.accountId = request.accountId();
        this.amount = calculateCashbackAmount(request);
        this.transactionId = request.transactionId();
        this.walletCredited = false;
        this.cashbackRecorded = false;
    }

    private BigDecimal calculateCashbackAmount(CreateCashbackRequest request) {
        double percentage = 0.01;

        if (request.categoryCode() != null) {
            percentage = switch (request.categoryCode().toUpperCase()) {
                case "GROCERY" -> 0.02;
                case "DINING" -> 0.03;
                case "SHOPPING" -> 0.015;
                default -> 0.01;
            };
        }

        return request.transactionAmount().multiply(BigDecimal.valueOf(percentage))
            .setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    // Getters and Setters
    public CreateCashbackRequest getRequest() {
        return request;
    }

    public void setRequest(CreateCashbackRequest request) {
        this.request = request;
    }

    public Cashback getCashback() {
        return cashback;
    }

    public void setCashback(Cashback cashback) {
        this.cashback = cashback;
    }

    public UUID getCashbackId() {
        return cashbackId;
    }

    public void setCashbackId(UUID cashbackId) {
        this.cashbackId = cashbackId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public boolean isWalletCredited() {
        return walletCredited;
    }

    public void setWalletCredited(boolean walletCredited) {
        this.walletCredited = walletCredited;
    }

    public boolean isCashbackRecorded() {
        return cashbackRecorded;
    }

    public void setCashbackRecorded(boolean cashbackRecorded) {
        this.cashbackRecorded = cashbackRecorded;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
