package id.payu.billing.adapter.persistence;

import id.payu.billing.adapter.persistence.repository.SubscriptionChargeRepository;
import id.payu.billing.adapter.persistence.repository.SubscriptionPlanRepository;
import id.payu.billing.adapter.persistence.repository.SubscriptionRepository;
import id.payu.billing.adapter.persistence.entity.SubscriptionEntity;
import id.payu.billing.adapter.persistence.entity.SubscriptionChargeEntity;
import id.payu.billing.adapter.persistence.entity.SubscriptionPlanEntity;
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
    public SubscriptionPlanEntity savePlan(SubscriptionPlanEntity plan) {
        return planRepository.save(plan);
    }

    @Override
    public Optional<SubscriptionPlanEntity> findPlanById(UUID id) {
        return planRepository.findById(id);
    }

    @Override
    public List<SubscriptionPlanEntity> findPlansByPartnerId(String partnerId) {
        return planRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId);
    }

    @Override
    public SubscriptionEntity saveSubscription(SubscriptionEntity subscription) {
        return subscriptionRepository.save(subscription);
    }

    @Override
    public Optional<SubscriptionEntity> findSubscriptionById(UUID id) {
        return subscriptionRepository.findById(id);
    }

    @Override
    public List<SubscriptionEntity> findSubscriptionsByAccountId(String accountId) {
        return subscriptionRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    @Override
    public List<SubscriptionEntity> findSubscriptionsByPartnerId(String partnerId) {
        return subscriptionRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId);
    }

    @Override
    public List<SubscriptionEntity> findDueSubscriptions(LocalDateTime cutoff) {
        return subscriptionRepository.findDueSubscriptions(cutoff);
    }

    @Override
    public List<SubscriptionEntity> findPastDueSubscriptions() {
        return subscriptionRepository.findPastDueSubscriptions();
    }

    @Override
    public List<SubscriptionEntity> findExpiredTrials(LocalDateTime now) {
        return subscriptionRepository.findExpiredTrials(now);
    }

    @Override
    public SubscriptionChargeEntity saveCharge(SubscriptionChargeEntity charge) {
        return chargeRepository.save(charge);
    }

    @Override
    public Optional<SubscriptionChargeEntity> findChargeByIdempotencyKey(String idempotencyKey) {
        return chargeRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public List<SubscriptionChargeEntity> findChargesBySubscriptionId(UUID subscriptionId) {
        return chargeRepository.findBySubscriptionIdOrderByCreatedAtDesc(subscriptionId);
    }
}
