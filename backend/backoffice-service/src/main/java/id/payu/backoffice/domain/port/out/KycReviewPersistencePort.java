package id.payu.backoffice.domain.port.out;

import id.payu.backoffice.adapter.persistence.entity.KycReviewEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.backoffice.domain.KycStatus;

/**
 * Outbound port for KYC Review persistence.
 */
public interface KycReviewPersistencePort {

    KycReviewEntity save(KycReviewEntity kycReview);

    Optional<KycReviewEntity> findById(UUID id);

    List<KycReviewEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    List<KycReviewEntity> findByStatus(KycStatus status, int page, int size);

    List<KycReviewEntity> findAll(int page, int size);

    List<KycReviewEntity> findByUserIdContainingIgnoreCase(String query);

    List<KycReviewEntity> findByAccountNumberContainingIgnoreCase(String query);

    List<KycReviewEntity> findByDocumentNumberContainingIgnoreCase(String query);

    List<KycReviewEntity> findByFullNameContainingIgnoreCase(String query);

    void deleteById(UUID id);
}
