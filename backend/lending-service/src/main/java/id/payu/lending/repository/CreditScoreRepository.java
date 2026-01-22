package id.payu.lending.repository;

import id.payu.lending.entity.CreditScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditScoreRepository extends JpaRepository<CreditScoreEntity, UUID> {
    Optional<CreditScoreEntity> findByUserId(UUID userId);
}
