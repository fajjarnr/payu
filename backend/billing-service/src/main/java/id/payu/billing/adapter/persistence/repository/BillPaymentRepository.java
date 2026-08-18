package id.payu.billing.adapter.persistence.repository;

import id.payu.billing.infrastructure.persistence.entity.BillPaymentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * BE-BILL-001: paginated payment history for an account (newest first).
     */
    Page<BillPaymentEntity> findByAccountIdOrderByCreatedAtDesc(String accountId, Pageable pageable);

    /**
     * BILL-RECON-001: only payments that have not yet published their event
     * need reconciliation — completed/failed rows with event_published=true
     * are terminal and must not be re-scanned every 60s.
     */
    List<BillPaymentEntity> findByStatusInAndEventPublishedFalse(Collection<String> statuses);
}
