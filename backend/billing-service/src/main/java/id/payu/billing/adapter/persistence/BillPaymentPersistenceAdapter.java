package id.payu.billing.adapter.persistence;

import id.payu.billing.adapter.persistence.repository.BillPaymentRepository;
import id.payu.billing.domain.model.BillPayment;
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
    public BillPayment save(BillPayment payment) {
        return billPaymentRepository.save(payment);
    }

    @Override
    public Optional<BillPayment> findById(UUID id) {
        return billPaymentRepository.findById(id);
    }

    @Override
    public Optional<BillPayment> findByReferenceNumber(String referenceNumber) {
        return billPaymentRepository.findByReferenceNumber(referenceNumber);
    }
}
