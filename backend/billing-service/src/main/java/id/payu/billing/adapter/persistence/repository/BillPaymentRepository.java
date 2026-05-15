package id.payu.billing.adapter.persistence.repository;

import id.payu.billing.adapter.persistence.entity.BillPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for BillPaymentEntity entity.
 */
@Repository
public interface BillPaymentRepository extends JpaRepository<BillPaymentEntity, UUID> {

    /**
     * Find payment by reference number.
     */
    Optional<BillPaymentEntity> findByReferenceNumber(String referenceNumber);
}
