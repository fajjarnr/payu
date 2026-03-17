package id.payu.wallet.adapter.persistence.repository;

import id.payu.wallet.adapter.persistence.entity.WalletEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for WalletEntity.
 */
@Repository
public interface WalletJpaRepository extends JpaRepository<WalletEntity, UUID> {

    Optional<WalletEntity> findByAccountId(String accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("SELECT w FROM WalletEntity w WHERE w.accountId = :accountId")
    Optional<WalletEntity> findByAccountIdForUpdate(@Param("accountId") String accountId);
}
