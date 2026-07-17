package id.payu.backoffice.adapter.persistence.repository;

import id.payu.backoffice.adapter.persistence.entity.FraudCaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;
import id.payu.backoffice.domain.FraudCaseStatus;

@Repository
public interface FraudCaseRepository extends JpaRepository<FraudCaseEntity, UUID> {
    List<FraudCaseEntity> findByUserId(String userId);
    List<FraudCaseEntity> findByStatus(FraudCaseStatus status);
    Page<FraudCaseEntity> findByStatus(FraudCaseStatus status, Pageable pageable);
    Page<FraudCaseEntity> findByRiskLevel(id.payu.backoffice.domain.RiskLevel riskLevel, Pageable pageable);

    // Search methods
    List<FraudCaseEntity> findByUserIdContainingIgnoreCase(String userId);
    List<FraudCaseEntity> findByAccountNumberContainingIgnoreCase(String accountNumber);
    List<FraudCaseEntity> findByFraudTypeContainingIgnoreCase(String fraudType);
}
