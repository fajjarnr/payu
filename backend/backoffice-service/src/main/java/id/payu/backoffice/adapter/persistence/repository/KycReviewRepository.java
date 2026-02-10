package id.payu.backoffice.adapter.persistence.repository;

import id.payu.backoffice.domain.KycReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KycReviewRepository extends JpaRepository<KycReview, UUID> {
    List<KycReview> findByStatus(KycReview.KycStatus status);
    List<KycReview> findByUserId(String userId);

    // Search methods
    List<KycReview> findByUserIdContainingIgnoreCase(String userId);
    List<KycReview> findByAccountNumberContainingIgnoreCase(String accountNumber);
    List<KycReview> findByDocumentNumberContainingIgnoreCase(String documentNumber);
    List<KycReview> findByFullNameContainingIgnoreCase(String fullName);
}
