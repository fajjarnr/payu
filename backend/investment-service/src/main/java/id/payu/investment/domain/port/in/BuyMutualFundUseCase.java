package id.payu.investment.domain.port.in;

import id.payu.investment.domain.model.InvestmentTransaction;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public interface BuyMutualFundUseCase {
    CompletableFuture<InvestmentTransaction> buyMutualFund(String accountId, String userId, 
            String fundCode, BigDecimal amount);
}
