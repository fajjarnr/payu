package id.payu.backoffice.dto;

import jakarta.validation.constraints.NotNull;

public record KycReviewDecisionRequest(
        @NotNull(message = "Status is required")
        KycReviewStatus status,

        String notes
) {
    public enum KycReviewStatus {
        APPROVED,
        REJECTED,
        REQUIRES_ADDITIONAL_INFO
    }
}
