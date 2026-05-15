package id.payu.backoffice.adapter.persistence.repository;

import id.payu.backoffice.adapter.persistence.entity.KycReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;
import id.payu.backoffice.domain.KycStatus;

@Repository
public interface KycReviewRepository extends JpaRepository<KycReviewEntity, UUID> {
    List<KycReviewEntity> findByStatus(KycStatus status);
    // BUG-BE-043: Pageable version
    Page<KycReviewEntity> findByStatus(KycStatus status, Pageable pageable);
    List<KycReviewEntity> findByUserId(String userId);
    List<KycReviewEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    // Search methods
    List<KycReviewEntity> findByUserIdContainingIgnoreCase(String userId);
    List<KycReviewEntity> findByAccountNumberContainingIgnoreCase(String accountNumber);
    List<KycReviewEntity> findByDocumentNumberContainingIgnoreCase(String documentNumber);
    List<KycReviewEntity> findByFullNameContainingIgnoreCase(String fullName);
}
