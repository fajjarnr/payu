package id.payu.billing.domain.port.in;

import id.payu.billing.domain.model.Subscription;
import id.payu.billing.domain.model.SubscriptionCharge;
import id.payu.billing.domain.model.SubscriptionPlan;
import id.payu.billing.domain.model.SubscriptionActor;

import java.util.List;
import java.util.UUID;
import id.payu.billing.domain.model.BillingInterval;

/**
 * Input port for subscription and recurring billing use cases.
 */
public interface SubscriptionUseCase {

    // --- Plan Management ---

    SubscriptionPlan createPlan(SubscriptionActor actor, String partnerId, String planName, String description,
                                 BillingInterval interval,
                                 java.math.BigDecimal price, String currency,
                                 int trialDays, int gracePeriodDays);

    SubscriptionPlan getPlan(SubscriptionActor actor, UUID planId);

    List<SubscriptionPlan> getPlansByPartner(SubscriptionActor actor, String partnerId);

    void deactivatePlan(SubscriptionActor actor, UUID planId);

    // --- Subscription Lifecycle ---

    Subscription subscribe(SubscriptionActor actor, String accountId, UUID planId, String externalReferenceId);

    Subscription getSubscription(SubscriptionActor actor, UUID subscriptionId);

    List<Subscription> getSubscriptionsByAccount(SubscriptionActor actor, String accountId);

    List<Subscription> getSubscriptionsByPartner(SubscriptionActor actor, String partnerId);

    Subscription cancelSubscription(SubscriptionActor actor, UUID subscriptionId, String reason);

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

    void processScheduledCharge(UUID subscriptionId);

    List<SubscriptionCharge> getChargesBySubscription(SubscriptionActor actor, UUID subscriptionId);
}
