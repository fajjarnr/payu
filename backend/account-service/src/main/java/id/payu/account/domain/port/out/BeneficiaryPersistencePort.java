package id.payu.account.domain.port.out;

import id.payu.account.domain.model.Beneficiary;
import id.payu.account.domain.model.BeneficiaryStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BeneficiaryPersistencePort {

    Beneficiary save(Beneficiary beneficiary);

    Optional<Beneficiary> findById(UUID id);

    List<Beneficiary> findActiveByUserId(UUID userId);

    long countActiveByUserId(UUID userId);

    Optional<Beneficiary> findByUserIdAndBankCodeAndAccountNumber(UUID userId, String bankCode, String accountNumber);

    boolean existsByUserIdAndBankCodeAndAccountNumber(UUID userId, String bankCode, String accountNumber);
}
