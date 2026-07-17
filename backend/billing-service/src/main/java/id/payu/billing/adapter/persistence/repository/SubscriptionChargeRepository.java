package id.payu.billing.adapter.persistence.repository;

import id.payu.billing.infrastructure.persistence.entity.SubscriptionChargeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionChargeRepository extends JpaRepository<SubscriptionChargeEntity, UUID> {

    Optional<SubscriptionChargeEntity> findByIdempotencyKey(String idempotencyKey);

    List<SubscriptionChargeEntity> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId);
}
