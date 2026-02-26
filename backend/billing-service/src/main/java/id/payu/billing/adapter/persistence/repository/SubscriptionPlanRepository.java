package id.payu.billing.adapter.persistence.repository;

import id.payu.billing.domain.model.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {

    List<SubscriptionPlan> findByPartnerIdAndActiveTrue(String partnerId);

    List<SubscriptionPlan> findByPartnerIdOrderByCreatedAtDesc(String partnerId);
}
