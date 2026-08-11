package id.payu.partner.adapter.persistence.repository;

import id.payu.partner.adapter.persistence.entity.SnapReconciliationCaseEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * PARTNER-PROD-005: reconciliation case persistence.
 */
@Repository
public interface SnapReconciliationCaseRepository extends JpaRepository<SnapReconciliationCaseEntity, Long> {

    boolean existsByReferenceTypeAndReferenceId(String referenceType, String referenceId);

    Optional<SnapReconciliationCaseEntity> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);
}
