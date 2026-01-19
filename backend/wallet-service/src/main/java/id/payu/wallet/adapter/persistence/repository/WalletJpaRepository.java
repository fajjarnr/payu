package id.payu.wallet.adapter.persistence.repository;

import id.payu.wallet.adapter.persistence.entity.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for WalletEntity.
 */
@Repository
public interface WalletJpaRepository extends JpaRepository<WalletEntity, UUID> {

    Optional<WalletEntity> findByAccountId(String accountId);
}
