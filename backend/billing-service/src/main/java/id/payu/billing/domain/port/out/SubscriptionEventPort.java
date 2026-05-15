package id.payu.billing.domain.port.out;

import id.payu.billing.adapter.persistence.entity.SubscriptionEntity;
import id.payu.billing.adapter.persistence.entity.SubscriptionChargeEntity;

public interface SubscriptionEventPort {
    void publishSubscriptionCreated(SubscriptionEntity subscription);
    void publishChargeSucceeded(SubscriptionEntity subscription, SubscriptionChargeEntity charge);
    void publishChargeFailed(SubscriptionEntity subscription, SubscriptionChargeEntity charge);
}
