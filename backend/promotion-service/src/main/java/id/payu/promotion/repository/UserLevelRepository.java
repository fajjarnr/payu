package id.payu.promotion.repository;

import id.payu.promotion.domain.UserLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserLevelRepository extends JpaRepository<UserLevel, UUID> {

    Optional<UserLevel> findByAccountId(String accountId);

    boolean existsByAccountId(String accountId);
}
