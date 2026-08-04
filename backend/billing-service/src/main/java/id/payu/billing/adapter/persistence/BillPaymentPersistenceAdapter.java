package id.payu.billing.adapter.persistence;

import id.payu.billing.adapter.persistence.repository.BillPaymentRepository;
import id.payu.billing.infrastructure.persistence.entity.BillPaymentEntity;
import id.payu.billing.domain.model.BillPayment;
import id.payu.billing.domain.model.PaymentStatus;
import id.payu.billing.domain.port.out.BillPaymentPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.beans.BeanUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import java.util.List;

/**
 * JPA adapter implementing the persistence port.
 */
@Component
@RequiredArgsConstructor
public class BillPaymentPersistenceAdapter implements BillPaymentPersistencePort {

    private final BillPaymentRepository billPaymentRepository;

    @Override
    public BillPayment save(BillPayment payment) {
        return toDomain(billPaymentRepository.save(toEntity(payment)));
    }

    @Override
    public Optional<BillPayment> findById(UUID id) {
        return billPaymentRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<BillPayment> findByReferenceNumber(String referenceNumber) {
        return billPaymentRepository.findByReferenceNumber(referenceNumber).map(this::toDomain);
    }

    @Override
    public Optional<BillPayment> findByIdempotencyKey(String idempotencyKey) {
        return billPaymentRepository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    public List<BillPayment> findByStatusIn(Collection<PaymentStatus> statuses) {
        return billPaymentRepository.findByStatusIn(statuses.stream().map(Enum::name).toList())
                .stream().map(this::toDomain).toList();
    }

    private BillPaymentEntity toEntity(BillPayment payment) {
        BillPaymentEntity entity = payment.getId() == null
                ? new BillPaymentEntity()
                : billPaymentRepository.findById(payment.getId()).orElseGet(BillPaymentEntity::new);
        BeanUtils.copyProperties(payment, entity);
        entity.setBillerType(payment.getBillerType().name());
        entity.setStatus(payment.getStatus().name());
        entity.setIdempotencyKey(payment.getIdempotencyKey());
        entity.setWalletReservationId(payment.getWalletReservationId());
        entity.setEventPublished(payment.isEventPublished());
        return entity;
    }

    private BillPayment toDomain(BillPaymentEntity entity) {
        BillPayment payment = new BillPayment();
        BeanUtils.copyProperties(entity, payment);
        payment.setBillerType(id.payu.billing.domain.model.BillerType.valueOf(entity.getBillerType()));
        payment.setStatus(id.payu.billing.domain.model.PaymentStatus.valueOf(entity.getStatus()));
        payment.setIdempotencyKey(entity.getIdempotencyKey());
        payment.setWalletReservationId(entity.getWalletReservationId());
        payment.setEventPublished(entity.isEventPublished());
        return payment;
    }
}
