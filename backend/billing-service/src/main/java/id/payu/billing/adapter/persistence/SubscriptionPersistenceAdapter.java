package id.payu.billing.adapter.persistence;

import id.payu.billing.adapter.persistence.repository.SubscriptionChargeRepository;
import id.payu.billing.adapter.persistence.repository.SubscriptionPlanRepository;
import id.payu.billing.adapter.persistence.repository.SubscriptionRepository;
import id.payu.billing.domain.model.Subscription;
import id.payu.billing.domain.model.SubscriptionCharge;
import id.payu.billing.domain.model.SubscriptionPlan;
import id.payu.billing.domain.port.out.SubscriptionPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SubscriptionPersistenceAdapter implements SubscriptionPersistencePort {

    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionChargeRepository chargeRepository;

    @Override
    public SubscriptionPlan savePlan(SubscriptionPlan plan) {
        return planRepository.save(plan);
    }

    @Override
    public Optional<SubscriptionPlan> findPlanById(UUID id) {
        return planRepository.findById(id);
    }

    @Override
    public List<SubscriptionPlan> findPlansByPartnerId(String partnerId) {
        return planRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId);
    }

    @Override
    public Subscription saveSubscription(Subscription subscription) {
        return subscriptionRepository.save(subscription);
    }

    @Override
    public Optional<Subscription> findSubscriptionById(UUID id) {
        return subscriptionRepository.findById(id);
    }

    @Override
    public List<Subscription> findSubscriptionsByAccountId(String accountId) {
        return subscriptionRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    @Override
    public List<Subscription> findSubscriptionsByPartnerId(String partnerId) {
        return subscriptionRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId);
    }

    @Override
    public List<Subscription> findDueSubscriptions(LocalDateTime cutoff) {
        return subscriptionRepository.findDueSubscriptions(cutoff);
    }

    @Override
    public List<Subscription> findPastDueSubscriptions() {
        return subscriptionRepository.findPastDueSubscriptions();
    }

    @Override
    public List<Subscription> findExpiredTrials(LocalDateTime now) {
        return subscriptionRepository.findExpiredTrials(now);
    }

    @Override
    public SubscriptionCharge saveCharge(SubscriptionCharge charge) {
        return chargeRepository.save(charge);
    }

    @Override
    public Optional<SubscriptionCharge> findChargeByIdempotencyKey(String idempotencyKey) {
        return chargeRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public List<SubscriptionCharge> findChargesBySubscriptionId(UUID subscriptionId) {
        return chargeRepository.findBySubscriptionIdOrderByCreatedAtDesc(subscriptionId);
    }
}
