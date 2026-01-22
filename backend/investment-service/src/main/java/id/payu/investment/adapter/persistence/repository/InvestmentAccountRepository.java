package id.payu.investment.adapter.persistence.repository;

import id.payu.investment.adapter.persistence.InvestmentAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvestmentAccountRepository extends JpaRepository<InvestmentAccountEntity, UUID> {
    Optional<InvestmentAccountEntity> findByUserId(String userId);
    boolean existsByUserId(String userId);
}
