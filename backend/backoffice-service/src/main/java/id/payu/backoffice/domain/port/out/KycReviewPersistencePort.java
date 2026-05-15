package id.payu.backoffice.domain.port.out;

import id.payu.backoffice.domain.KycReview;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for KYC Review persistence.
 */
public interface KycReviewPersistencePort {

    KycReview save(KycReview kycReview);

    Optional<KycReview> findById(UUID id);

    List<KycReview> findByUserIdOrderByCreatedAtDesc(String userId);

    List<KycReview> findByStatus(KycReview.KycStatus status, int page, int size);

    List<KycReview> findAll(int page, int size);

    List<KycReview> findByUserIdContainingIgnoreCase(String query);

    List<KycReview> findByAccountNumberContainingIgnoreCase(String query);

    List<KycReview> findByDocumentNumberContainingIgnoreCase(String query);

    List<KycReview> findByFullNameContainingIgnoreCase(String query);

    void deleteById(UUID id);
}
