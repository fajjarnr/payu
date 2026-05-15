package id.payu.backoffice.domain.port.in;

import id.payu.backoffice.domain.KycReview;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Inbound port for KYC Review use cases.
 */
public interface KycReviewUseCase {

    KycReview create(String userId, String accountNumber, String documentType,
                     String documentNumber, String documentUrl, String fullName,
                     String address, String phoneNumber, String notes);

    Optional<KycReview> getById(UUID id);

    Optional<KycReview> getByUserId(String userId);

    List<KycReview> listByStatus(KycReview.KycStatus status, int page, int size);

    List<KycReview> listAll(int page, int size);

    KycReview review(UUID id, KycReview.KycStatus decision, String notes, String reviewedBy);

    void delete(UUID id);
}
