package id.payu.billing.domain.port.out;

import id.payu.billing.adapter.persistence.entity.SubscriptionEntity;
import id.payu.billing.adapter.persistence.entity.SubscriptionChargeEntity;
import id.payu.billing.adapter.persistence.entity.SubscriptionPlanEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPersistencePort {
    SubscriptionPlanEntity savePlan(SubscriptionPlanEntity plan);
    Optional<SubscriptionPlanEntity> findPlanById(UUID id);
    List<SubscriptionPlanEntity> findPlansByPartnerId(String partnerId);

    SubscriptionEntity saveSubscription(SubscriptionEntity subscription);
    Optional<SubscriptionEntity> findSubscriptionById(UUID id);
    List<SubscriptionEntity> findSubscriptionsByAccountId(String accountId);
    List<SubscriptionEntity> findSubscriptionsByPartnerId(String partnerId);
    List<SubscriptionEntity> findDueSubscriptions(LocalDateTime cutoff);
    List<SubscriptionEntity> findPastDueSubscriptions();
    List<SubscriptionEntity> findExpiredTrials(LocalDateTime now);

    SubscriptionChargeEntity saveCharge(SubscriptionChargeEntity charge);
    Optional<SubscriptionChargeEntity> findChargeByIdempotencyKey(String idempotencyKey);
    List<SubscriptionChargeEntity> findChargesBySubscriptionId(UUID subscriptionId);
}
