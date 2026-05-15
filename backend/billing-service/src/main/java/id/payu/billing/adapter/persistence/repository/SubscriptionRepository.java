package id.payu.billing.adapter.persistence.repository;

import id.payu.billing.adapter.persistence.entity.SubscriptionEntity;
import id.payu.billing.domain.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {

    List<SubscriptionEntity> findByAccountIdOrderByCreatedAtDesc(String accountId);

    List<SubscriptionEntity> findByPartnerIdOrderByCreatedAtDesc(String partnerId);

    @Query("SELECT s FROM SubscriptionEntity s WHERE s.status = 'ACTIVE' AND s.nextBillingAt <= :cutoff")
    List<SubscriptionEntity> findDueSubscriptions(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT s FROM SubscriptionEntity s WHERE s.status = 'PAST_DUE'")
    List<SubscriptionEntity> findPastDueSubscriptions();

    @Query("SELECT s FROM SubscriptionEntity s WHERE s.status = 'TRIAL' AND s.trialEndAt <= :now")
    List<SubscriptionEntity> findExpiredTrials(@Param("now") LocalDateTime now);
}
