package id.payu.account.domain.port.out;

import id.payu.account.domain.model.Account;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for account persistence operations.
 * This interface follows the hexagonal architecture pattern.
 */
public interface AccountPersistencePort {
    Account save(Account account);
    Optional<Account> findById(UUID id);
    Optional<Account> findByExternalId(String externalId);
    boolean existsByAccountNumber(String accountNumber);
}
