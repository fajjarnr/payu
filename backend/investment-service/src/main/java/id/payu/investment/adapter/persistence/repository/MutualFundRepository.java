package id.payu.investment.adapter.persistence.repository;

import id.payu.investment.adapter.persistence.MutualFundEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MutualFundRepository extends JpaRepository<MutualFundEntity, java.util.UUID> {
    Optional<MutualFundEntity> findByCode(String code);
}
