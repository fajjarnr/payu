package id.payu.backoffice.dto;

import id.payu.backoffice.domain.KycReview;
import java.time.LocalDateTime;
import java.util.UUID;

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
        KycReview.KycStatus status,
        String notes,
        String reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
) {
    public static KycReviewResponse from(KycReview review) {
        return new KycReviewResponse(
                review.id,
                review.userId,
                review.accountNumber,
                review.documentType,
                review.documentNumber,
                review.documentUrl,
                review.fullName,
                review.address,
                review.phoneNumber,
                review.status,
                review.notes,
                review.reviewedBy,
                review.reviewedAt,
                review.createdAt
        );
    }
}
