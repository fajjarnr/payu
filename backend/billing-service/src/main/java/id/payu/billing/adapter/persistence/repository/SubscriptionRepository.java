package id.payu.billing.adapter.persistence.repository;

import id.payu.billing.domain.model.Subscription;
import id.payu.billing.domain.model.Subscription.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    List<Subscription> findByAccountIdOrderByCreatedAtDesc(String accountId);

    List<Subscription> findByPartnerIdOrderByCreatedAtDesc(String partnerId);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.nextBillingAt <= :cutoff")
    List<Subscription> findDueSubscriptions(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'PAST_DUE'")
    List<Subscription> findPastDueSubscriptions();

    @Query("SELECT s FROM Subscription s WHERE s.status = 'TRIAL' AND s.trialEndAt <= :now")
    List<Subscription> findExpiredTrials(@Param("now") LocalDateTime now);
}
