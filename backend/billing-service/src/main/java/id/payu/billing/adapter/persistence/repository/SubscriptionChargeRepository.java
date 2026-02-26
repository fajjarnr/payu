package id.payu.billing.adapter.persistence.repository;

import id.payu.billing.domain.model.SubscriptionCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionChargeRepository extends JpaRepository<SubscriptionCharge, UUID> {

    Optional<SubscriptionCharge> findByIdempotencyKey(String idempotencyKey);

    List<SubscriptionCharge> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId);
}
