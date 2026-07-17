package id.payu.billing.adapter.persistence.repository;

import id.payu.billing.infrastructure.persistence.entity.SubscriptionPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlanEntity, UUID> {

    List<SubscriptionPlanEntity> findByPartnerIdAndActiveTrue(String partnerId);

    List<SubscriptionPlanEntity> findByPartnerIdOrderByCreatedAtDesc(String partnerId);
}
