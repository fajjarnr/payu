package id.payu.billing.repository;

import id.payu.billing.domain.BillPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for BillPayment entity.
 */
@Repository
public interface BillPaymentRepository extends JpaRepository<BillPayment, UUID> {

    /**
     * Find payment by reference number.
     */
    Optional<BillPayment> findByReferenceNumber(String referenceNumber);
}
