package id.payu.investment.adapter.persistence.repository;

import id.payu.investment.adapter.persistence.GoldEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoldRepository extends JpaRepository<GoldEntity, java.util.UUID> {
    Optional<GoldEntity> findByUserId(String userId);
}
