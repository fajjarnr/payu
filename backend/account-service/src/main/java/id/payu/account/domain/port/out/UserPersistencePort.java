package id.payu.account.domain.port.out;

import id.payu.account.domain.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for user persistence operations.
 * ITER-54: Application services depend on this port, not on Spring Data repositories.
 */
public interface UserPersistencePort {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    /**
     * ITER-54: Added for READY-052 (Hexagonal cleanup) so application services
     * can resolve user by Keycloak externalId without coupling to UserRepository.
     */
    Optional<User> findByExternalId(String externalId);

    /**
     * ITER-54: Returns list of account IDs owned by the given user.
     */
    List<UUID> findAccountIdsByUserId(UUID userId);
}
