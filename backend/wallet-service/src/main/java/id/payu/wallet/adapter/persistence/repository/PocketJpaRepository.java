package id.payu.wallet.adapter.persistence.repository;

import id.payu.wallet.adapter.persistence.entity.PocketEntity;
import id.payu.wallet.domain.model.Pocket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PocketJpaRepository extends JpaRepository<PocketEntity, UUID> {

    Optional<PocketEntity> findByAccountId(String accountId);

    List<PocketEntity> findByAccountIdAndCurrency(String accountId, String currency);

    @Query("SELECT p FROM PocketEntity p WHERE p.status = 'ACTIVE'")
    List<PocketEntity> findAllActive();
}
