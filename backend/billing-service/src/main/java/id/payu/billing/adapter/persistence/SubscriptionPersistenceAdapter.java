package id.payu.billing.adapter.persistence;

import id.payu.billing.adapter.persistence.repository.SubscriptionChargeRepository;
import id.payu.billing.adapter.persistence.repository.SubscriptionPlanRepository;
import id.payu.billing.adapter.persistence.repository.SubscriptionRepository;
import id.payu.billing.infrastructure.persistence.entity.SubscriptionEntity;
import id.payu.billing.infrastructure.persistence.entity.SubscriptionChargeEntity;
import id.payu.billing.infrastructure.persistence.entity.SubscriptionPlanEntity;
import id.payu.billing.domain.model.Subscription;
import id.payu.billing.domain.model.SubscriptionCharge;
import id.payu.billing.domain.model.SubscriptionPlan;
import id.payu.billing.domain.port.out.SubscriptionPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.beans.BeanUtils;

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
        return toDomain(planRepository.save(toEntity(plan)));
    }

    @Override
    public Optional<SubscriptionPlan> findPlanById(UUID id) {
        return planRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<SubscriptionPlan> findPlansByPartnerId(String partnerId) {
        return planRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId).stream().map(this::toDomain).toList();
    }

    @Override
    public Subscription saveSubscription(Subscription subscription) {
        return toDomain(subscriptionRepository.save(toEntity(subscription)));
    }

    @Override
    public Optional<Subscription> findSubscriptionById(UUID id) {
        return subscriptionRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Subscription> findSubscriptionsByAccountId(String accountId) {
        return subscriptionRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Subscription> findSubscriptionsByPartnerId(String partnerId) {
        return subscriptionRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Subscription> findDueSubscriptions(LocalDateTime cutoff) {
        return subscriptionRepository.findDueSubscriptions(cutoff).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Subscription> findPastDueSubscriptions() {
        return subscriptionRepository.findPastDueSubscriptions().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Subscription> findExpiredTrials(LocalDateTime now) {
        return subscriptionRepository.findExpiredTrials(now).stream().map(this::toDomain).toList();
    }

    @Override
    public SubscriptionCharge saveCharge(SubscriptionCharge charge) {
        return toDomain(chargeRepository.save(toEntity(charge)));
    }

    @Override
    public Optional<SubscriptionCharge> findChargeByIdempotencyKey(String idempotencyKey) {
        return chargeRepository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    public List<SubscriptionCharge> findChargesBySubscriptionId(UUID subscriptionId) {
        return chargeRepository.findBySubscriptionIdOrderByCreatedAtDesc(subscriptionId).stream().map(this::toDomain).toList();
    }

    private SubscriptionPlanEntity toEntity(SubscriptionPlan source) {
        SubscriptionPlanEntity target = copy(source, new SubscriptionPlanEntity());
        target.setBillingInterval(source.getBillingInterval().name());
        return target;
    }

    private SubscriptionPlan toDomain(SubscriptionPlanEntity source) {
        SubscriptionPlan target = copy(source, new SubscriptionPlan());
        target.setBillingInterval(id.payu.billing.domain.model.BillingInterval.valueOf(source.getBillingInterval()));
        return target;
    }

    private SubscriptionEntity toEntity(Subscription source) {
        SubscriptionEntity target = copy(source, new SubscriptionEntity());
        target.setStatus(source.getStatus().name());
        return target;
    }

    private Subscription toDomain(SubscriptionEntity source) {
        Subscription target = copy(source, new Subscription());
        target.setStatus(id.payu.billing.domain.model.SubscriptionStatus.valueOf(source.getStatus()));
        return target;
    }

    private SubscriptionChargeEntity toEntity(SubscriptionCharge source) {
        SubscriptionChargeEntity target = copy(source, new SubscriptionChargeEntity());
        target.setStatus(source.getStatus().name());
        return target;
    }

    private SubscriptionCharge toDomain(SubscriptionChargeEntity source) {
        SubscriptionCharge target = copy(source, new SubscriptionCharge());
        target.setStatus(id.payu.billing.domain.model.ChargeStatus.valueOf(source.getStatus()));
        return target;
    }

    private <T> T copy(Object source, T target) {
        BeanUtils.copyProperties(source, target);
        return target;
    }
}
