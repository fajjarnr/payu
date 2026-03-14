package id.payu.fx.domain.port.out;

import java.math.BigDecimal;

public interface WalletServicePort {
    boolean debit(String accountId, String transactionId, BigDecimal amount, String currency);
    boolean credit(String accountId, String transactionId, BigDecimal amount, String currency);
    void reverseDebit(String accountId, String transactionId, BigDecimal amount, String currency);
}
