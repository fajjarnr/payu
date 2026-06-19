package id.payu.backoffice.adapter.persistence.repository;

import id.payu.backoffice.adapter.persistence.entity.CustomerCaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;
import id.payu.backoffice.domain.CustomerCaseStatus;

@Repository
public interface CustomerCaseRepository extends JpaRepository<CustomerCaseEntity, UUID> {
    List<CustomerCaseEntity> findByUserId(String userId);
    List<CustomerCaseEntity> findByStatus(CustomerCaseStatus status);
    Page<CustomerCaseEntity> findByStatus(CustomerCaseStatus status, Pageable pageable);
    List<CustomerCaseEntity> findByAssignedTo(String assignedTo);

    // Search methods
    List<CustomerCaseEntity> findByUserIdContainingIgnoreCase(String userId);
    List<CustomerCaseEntity> findByAccountNumberContainingIgnoreCase(String accountNumber);
    List<CustomerCaseEntity> findByCaseNumberContainingIgnoreCase(String caseNumber);
    List<CustomerCaseEntity> findBySubjectContainingIgnoreCase(String subject);
}
