package id.payu.backoffice.domain.port.in;

import id.payu.backoffice.adapter.persistence.entity.KycReviewEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.backoffice.domain.KycStatus;

/**
 * Inbound port for KYC Review use cases.
 */
public interface KycReviewUseCase {

    KycReviewEntity create(String userId, String accountNumber, String documentType,
                     String documentNumber, String documentUrl, String fullName,
                     String address, String phoneNumber, String notes);

    Optional<KycReviewEntity> getById(UUID id);

    Optional<KycReviewEntity> getByUserId(String userId);

    List<KycReviewEntity> listByStatus(KycStatus status, int page, int size);

    List<KycReviewEntity> listAll(int page, int size);

    KycReviewEntity review(UUID id, KycStatus decision, String notes, String reviewedBy);

    void delete(UUID id);
}
