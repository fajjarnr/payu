package id.payu.billing.domain.port.out;

import id.payu.billing.domain.model.Subscription;
import id.payu.billing.domain.model.SubscriptionCharge;
import id.payu.billing.domain.model.SubscriptionPlan;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPersistencePort {
    SubscriptionPlan savePlan(SubscriptionPlan plan);
    Optional<SubscriptionPlan> findPlanById(UUID id);
    List<SubscriptionPlan> findPlansByPartnerId(String partnerId);

    Subscription saveSubscription(Subscription subscription);
    Optional<Subscription> findSubscriptionById(UUID id);
    List<Subscription> findSubscriptionsByAccountId(String accountId);
    List<Subscription> findSubscriptionsByPartnerId(String partnerId);
    List<Subscription> findDueSubscriptions(LocalDateTime cutoff);
    List<Subscription> findPastDueSubscriptions();
    List<Subscription> findExpiredTrials(LocalDateTime now);

    SubscriptionCharge saveCharge(SubscriptionCharge charge);
    Optional<SubscriptionCharge> findChargeByIdempotencyKey(String idempotencyKey);
    List<SubscriptionCharge> findChargesBySubscriptionId(UUID subscriptionId);
}
