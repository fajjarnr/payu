package id.payu.backoffice.dto;

import id.payu.backoffice.adapter.persistence.entity.KycReviewEntity;
import java.time.LocalDateTime;
import java.util.UUID;
import id.payu.backoffice.domain.KycStatus;

public record KycReviewResponse(
        UUID id,
        String userId,
        String accountNumber,
        String documentType,
        String documentNumber,
        String documentUrl,
        String fullName,
        String address,
        String phoneNumber,
        KycStatus status,
        String notes,
        String reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
) {
    public static KycReviewResponse from(KycReviewEntity review) {
        return new KycReviewResponse(
                review.getId(),
                review.getUserId(),
                review.getAccountNumber(),
                review.getDocumentType(),
                review.getDocumentNumber(),
                review.getDocumentUrl(),
                review.getFullName(),
                review.getAddress(),
                review.getPhoneNumber(),
                review.getStatus(),
                review.getNotes(),
                review.getReviewedBy(),
                review.getReviewedAt(),
                review.getCreatedAt()
        );
    }
}
