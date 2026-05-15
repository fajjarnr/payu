package id.payu.billing.adapter.persistence;

import id.payu.billing.adapter.persistence.repository.BillPaymentRepository;
import id.payu.billing.adapter.persistence.entity.BillPaymentEntity;
import id.payu.billing.domain.port.out.BillPaymentPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter implementing the persistence port.
 */
@Component
@RequiredArgsConstructor
public class BillPaymentPersistenceAdapter implements BillPaymentPersistencePort {

    private final BillPaymentRepository billPaymentRepository;

    @Override
    public BillPaymentEntity save(BillPaymentEntity payment) {
        return billPaymentRepository.save(payment);
    }

    @Override
    public Optional<BillPaymentEntity> findById(UUID id) {
        return billPaymentRepository.findById(id);
    }

    @Override
    public Optional<BillPaymentEntity> findByReferenceNumber(String referenceNumber) {
        return billPaymentRepository.findByReferenceNumber(referenceNumber);
    }
}
