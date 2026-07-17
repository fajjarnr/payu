package id.payu.promotion.domain.port.out;
import id.payu.promotion.domain.model.Cashback;
import java.util.*;
public interface CashbackPersistencePort {
    Cashback save(Cashback cashback);
    Optional<Cashback> findById(UUID id);
    List<Cashback> findByAccountId(String accountId);
}
