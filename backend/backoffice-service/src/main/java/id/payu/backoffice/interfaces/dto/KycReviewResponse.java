package id.payu.backoffice.interfaces.dto;

import id.payu.backoffice.domain.KycReview;
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
    public static KycReviewResponse from(KycReview review) {
        return new KycReviewResponse(
                review.getId(),
                PiiMasking.lastFour(review.getUserId()),
                PiiMasking.lastFour(review.getAccountNumber()),
                review.getDocumentType(),
                PiiMasking.lastFour(review.getDocumentNumber()),
                null,
                PiiMasking.name(review.getFullName()),
                "****",
                PiiMasking.lastFour(review.getPhoneNumber()),
                review.getStatus(),
                PiiMasking.redact(review.getNotes()),
                review.getReviewedBy(),
                review.getReviewedAt(),
                review.getCreatedAt()
        );
    }
}
