package id.payu.backoffice.dto;

import jakarta.validation.constraints.NotNull;
import id.payu.backoffice.domain.CustomerCase;

public record CustomerCaseUpdateRequest(
        @NotNull(message = "Status is required")
        CustomerCase.CaseStatus status,

        String notes
) {}
