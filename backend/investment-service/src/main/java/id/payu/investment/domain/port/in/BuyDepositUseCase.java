package id.payu.investment.domain.port.in;

import id.payu.investment.domain.model.Deposit;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public interface BuyDepositUseCase {
    CompletableFuture<Deposit> buyDeposit(String accountId, String userId, BigDecimal amount, int tenure);
}
