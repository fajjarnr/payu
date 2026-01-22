package id.payu.lending.repository;

import id.payu.lending.entity.PayLaterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayLaterRepository extends JpaRepository<PayLaterEntity, UUID> {
    Optional<PayLaterEntity> findByUserId(UUID userId);
}
