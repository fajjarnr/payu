package id.payu.promotion.domain.port.out;
import id.payu.promotion.domain.model.Cashback;
import java.util.*;
public interface CashbackPersistencePort {
    Cashback save(Cashback cashback);
    Optional<Cashback> findById(UUID id);

    /**
     * PROMO-001: replay lookup for the unique transaction_id guard.
     */
    Optional<Cashback> findByTransactionId(String transactionId);
    List<Cashback> findByAccountId(String accountId);
}
