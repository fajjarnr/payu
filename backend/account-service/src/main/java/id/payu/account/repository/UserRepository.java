package id.payu.account.repository;

import id.payu.account.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by ID.
     *
     * @param id the user ID
     * @return optional user
     */
    Optional<User> findById(UUID id);
}
