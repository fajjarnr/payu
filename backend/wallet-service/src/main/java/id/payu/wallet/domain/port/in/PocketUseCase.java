package id.payu.wallet.domain.port.in;

import id.payu.wallet.domain.model.Pocket;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PocketUseCase {

    Pocket createPocket(String accountId, String name, String description, String currency);

    Optional<Pocket> getPocketById(UUID pocketId);

    List<Pocket> getPocketsByAccountId(String accountId);

    List<Pocket> getPocketsByAccountIdAndCurrency(String accountId, String currency);

    void creditPocket(UUID pocketId, BigDecimal amount, String referenceId);

    void debitPocket(UUID pocketId, BigDecimal amount, String referenceId);

    void freezePocket(UUID pocketId);

    void unfreezePocket(UUID pocketId);

    void closePocket(UUID pocketId);

    BigDecimal getTotalBalanceInCurrency(String accountId, String targetCurrency);

    List<Pocket> getAllActivePockets();
}
