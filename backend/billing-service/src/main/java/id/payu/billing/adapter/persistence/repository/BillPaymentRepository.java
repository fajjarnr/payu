package id.payu.billing.adapter.persistence.repository;

import id.payu.billing.infrastructure.persistence.entity.BillPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import java.util.List;

/**
 * Spring Data JPA repository for BillPaymentEntity entity.
 */
@Repository
public interface BillPaymentRepository extends JpaRepository<BillPaymentEntity, UUID> {

    /**
     * Find payment by reference number.
     */
    Optional<BillPaymentEntity> findByReferenceNumber(String referenceNumber);

    Optional<BillPaymentEntity> findByIdempotencyKey(String idempotencyKey);

    List<BillPaymentEntity> findByStatusIn(Collection<String> statuses);
}
