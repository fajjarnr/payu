package id.payu.investment.adapter.persistence.repository;

import id.payu.investment.adapter.persistence.DepositEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepositRepository extends JpaRepository<DepositEntity, UUID> {
    Optional<DepositEntity> findByAccountId(String accountId);
}
