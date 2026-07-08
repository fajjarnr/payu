package id.payu.billing.domain.port.in;

import id.payu.billing.adapter.persistence.entity.SubscriptionEntity;
import id.payu.billing.adapter.persistence.entity.SubscriptionChargeEntity;
import id.payu.billing.adapter.persistence.entity.SubscriptionPlanEntity;

import java.util.List;
import java.util.UUID;
import id.payu.billing.domain.model.BillingInterval;

/**
 * Input port for subscription and recurring billing use cases.
 */
public interface SubscriptionUseCase {

    // --- Plan Management ---

    SubscriptionPlanEntity createPlan(String partnerId, String planName, String description,
                                 BillingInterval interval,
                                 java.math.BigDecimal price, String currency,
                                 int trialDays, int gracePeriodDays);

    SubscriptionPlanEntity getPlan(UUID planId);

    List<SubscriptionPlanEntity> getPlansByPartner(String partnerId);

    void deactivatePlan(UUID planId);

    // --- SubscriptionEntity Lifecycle ---

    SubscriptionEntity subscribe(String accountId, UUID planId, String externalReferenceId);

    SubscriptionEntity getSubscription(UUID subscriptionId);

    List<SubscriptionEntity> getSubscriptionsByAccount(String accountId);

    List<SubscriptionEntity> getSubscriptionsByPartner(String partnerId);

    SubscriptionEntity cancelSubscription(UUID subscriptionId, String reason);

    // --- Charging ---

    /**
     * Process all due subscriptions — called by scheduler.
     * Charges ACTIVE subscriptions where nextBillingAt <= now.
     * Retries PAST_DUE subscriptions (dunning).
     * Suspends after 3 failed attempts.
     */
    Integer processDueSubscriptions();

    /**
     * Transition trial-expired subscriptions to first charge.
     */
    Integer processExpiredTrials();

    List<SubscriptionChargeEntity> getChargesBySubscription(UUID subscriptionId);
}
