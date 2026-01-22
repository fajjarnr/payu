package id.payu.investment.domain.port.in;

import id.payu.investment.domain.model.InvestmentAccount;

import java.util.concurrent.CompletableFuture;

public interface CreateInvestmentAccountUseCase {
    CompletableFuture<InvestmentAccount> createAccount(String userId);
}
