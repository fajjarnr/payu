package id.payu.investment.domain.port.in;

import id.payu.investment.domain.model.Gold;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public interface BuyGoldUseCase {
    CompletableFuture<Gold> buyGold(String userId, BigDecimal amount);
}
