package id.payu.promotion.domain.port.out;

import id.payu.promotion.domain.TransactionType;

import java.math.BigDecimal;

public interface ReferralRewardPort {
    void grantCashback(String accountId, BigDecimal amount, String transactionId);
    void grantPoints(String accountId, int points, String transactionId, TransactionType type);
}
