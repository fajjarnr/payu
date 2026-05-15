package id.payu.backoffice.dto;

import jakarta.validation.constraints.NotNull;

public record FraudCaseDecisionRequest(
        @NotNull(message = "Status is required")
        FraudCaseStatus status,

        String notes
) {
}
