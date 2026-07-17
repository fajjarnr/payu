package id.payu.billing.domain.port.out;

import id.payu.billing.domain.model.Subscription;
import id.payu.billing.domain.model.SubscriptionCharge;

public interface SubscriptionEventPort {
    void publishSubscriptionCreated(Subscription subscription);
    void publishChargeSucceeded(Subscription subscription, SubscriptionCharge charge);
    void publishChargeFailed(Subscription subscription, SubscriptionCharge charge);
}
