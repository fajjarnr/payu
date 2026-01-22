package id.payu.lending.domain.port.out;

import id.payu.lending.domain.model.PayLaterTransaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayLaterTransactionPersistencePort {
    PayLaterTransaction save(PayLaterTransaction transaction);
    Optional<PayLaterTransaction> findById(UUID id);
    List<PayLaterTransaction> findByPayLaterAccountId(UUID paylaterAccountId);
    List<PayLaterTransaction> findByPayLaterAccountIdOrderByTransactionDateDesc(UUID paylaterAccountId);
    Optional<PayLaterTransaction> findByExternalId(String externalId);
}
