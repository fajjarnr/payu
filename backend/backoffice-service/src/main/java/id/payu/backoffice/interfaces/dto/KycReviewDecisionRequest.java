package id.payu.backoffice.interfaces.dto;

import jakarta.validation.constraints.NotNull;

public record KycReviewDecisionRequest(
        @NotNull(message = "Status is required")
        KycReviewStatus status,

        String notes
) {
}
