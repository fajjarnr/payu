package id.payu.lending.adapter.persistence;

import id.payu.lending.domain.model.PayLater;
import id.payu.lending.domain.port.out.PayLaterPersistencePort;
import id.payu.lending.entity.PayLaterEntity;
import id.payu.lending.repository.PayLaterRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Component
public class PayLaterPersistenceAdapter implements PayLaterPersistencePort {

    private final PayLaterRepository payLaterRepository;

    public PayLaterPersistenceAdapter(PayLaterRepository payLaterRepository) {
        this.payLaterRepository = payLaterRepository;
    }

    @Override
    public PayLater save(PayLater payLater) {
        PayLaterEntity entity = toEntity(payLater);
        PayLaterEntity saved = payLaterRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<PayLater> findByUserId(UUID userId) {
        return payLaterRepository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public Optional<PayLater> findByUserIdForUpdate(UUID userId) {
        return payLaterRepository.findByUserIdForUpdate(userId).map(this::toDomain);
    }

    @Override
    public Optional<PayLater> findById(UUID id) {
        return payLaterRepository.findById(id).map(this::toDomain);
    }

    private PayLater toDomain(PayLaterEntity entity) {
        PayLater payLater = new PayLater();
        payLater.setId(entity.getId());
        payLater.setExternalId(entity.getExternalId());
        payLater.setUserId(entity.getUserId());
        payLater.setCreditLimit(entity.getCreditLimit());
        payLater.setUsedCredit(entity.getUsedCredit());
        payLater.setAvailableCredit(entity.getAvailableCredit());
        payLater.setStatus(entity.getStatus());
        payLater.setBillingCycleDay(entity.getBillingCycleDay());
        payLater.setInterestRate(entity.getInterestRate());
        payLater.setCreatedAt(entity.getCreatedAt());
        payLater.setUpdatedAt(entity.getUpdatedAt());
        payLater.setVersion(entity.getVersion());
        return payLater;
    }

    private PayLaterEntity toEntity(PayLater payLater) {
        // Reload the existing managed entity when the ID is set to preserve
        // tenantId + version and keep the pessimistic lock from findByUserIdForUpdate
        PayLaterEntity entity = payLater.getId() == null
                ? new PayLaterEntity()
                : payLaterRepository.findById(payLater.getId())
                        .orElseThrow(() -> new IllegalStateException(
                                "PayLater not found: " + payLater.getId()));
        entity.setExternalId(payLater.getExternalId());
        entity.setUserId(payLater.getUserId());
        entity.setCreditLimit(payLater.getCreditLimit());
        entity.setUsedCredit(payLater.getUsedCredit());
        entity.setAvailableCredit(payLater.getAvailableCredit());
        entity.setStatus(payLater.getStatus());
        entity.setBillingCycleDay(payLater.getBillingCycleDay());
        entity.setInterestRate(payLater.getInterestRate());
        entity.setCreatedAt(payLater.getCreatedAt());
        entity.setUpdatedAt(payLater.getUpdatedAt());
        entity.setVersion(payLater.getVersion());
        return entity;
    }
}
