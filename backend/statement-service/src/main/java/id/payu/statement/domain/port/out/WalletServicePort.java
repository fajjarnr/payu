package id.payu.statement.domain.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface WalletServicePort {
    BigDecimal getCurrentBalance(String customerId);

    /**
     * IMP-3: ledger balance_after snapshot as of the end of the given day.
     * Empty when the customer has no ledger entries up to that date.
     */
    Optional<BigDecimal> getBalanceAsOf(String customerId, LocalDate endDate);
}
