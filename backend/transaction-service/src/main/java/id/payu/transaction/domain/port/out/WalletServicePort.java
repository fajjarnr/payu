package id.payu.transaction.domain.port.out;

import id.payu.transaction.dto.ReserveBalanceRequest;
import id.payu.transaction.dto.ReserveBalanceResponse;

import java.util.UUID;

public interface WalletServicePort {
    ReserveBalanceResponse reserveBalance(UUID accountId, String transactionId, java.math.BigDecimal amount);
    void commitBalance(UUID accountId, String transactionId, java.math.BigDecimal amount);
    void releaseBalance(UUID accountId, String transactionId, java.math.BigDecimal amount);
}
