package id.payu.investment.domain.port.in;

import id.payu.investment.domain.model.InvestmentTransaction;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SellInvestmentUseCase {
    CompletableFuture<InvestmentTransaction> sellInvestment(String accountId, UUID transactionId, BigDecimal amount);
}
