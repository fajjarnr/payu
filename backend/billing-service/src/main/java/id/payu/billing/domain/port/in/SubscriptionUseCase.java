package id.payu.billing.domain.port.in;

import id.payu.billing.domain.model.Subscription;
import id.payu.billing.domain.model.SubscriptionCharge;
import id.payu.billing.domain.model.SubscriptionPlan;

import java.util.List;
import java.util.UUID;

/**
 * Input port for subscription and recurring billing use cases.
 */
public interface SubscriptionUseCase {

    // --- Plan Management ---

    SubscriptionPlan createPlan(String partnerId, String planName, String description,
                                 SubscriptionPlan.BillingInterval interval,
                                 java.math.BigDecimal price, String currency,
                                 int trialDays, int gracePeriodDays);

    SubscriptionPlan getPlan(UUID planId);

    List<SubscriptionPlan> getPlansByPartner(String partnerId);

    void deactivatePlan(UUID planId);

    // --- Subscription Lifecycle ---

    Subscription subscribe(String accountId, UUID planId, String externalReferenceId);

    Subscription getSubscription(UUID subscriptionId);

    List<Subscription> getSubscriptionsByAccount(String accountId);

    List<Subscription> getSubscriptionsByPartner(String partnerId);

    Subscription cancelSubscription(UUID subscriptionId, String reason);

    // --- Charging ---

    /**
     * Process all due subscriptions — called by scheduler.
     * Charges ACTIVE subscriptions where nextBillingAt <= now.
     * Retries PAST_DUE subscriptions (dunning).
     * Suspends after 3 failed attempts.
     */
    int processDueSubscriptions();

    /**
     * Transition trial-expired subscriptions to first charge.
     */
    int processExpiredTrials();

    List<SubscriptionCharge> getChargesBySubscription(UUID subscriptionId);
}
