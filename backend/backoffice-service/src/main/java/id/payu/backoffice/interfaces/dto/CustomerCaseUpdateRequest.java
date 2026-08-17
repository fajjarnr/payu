package id.payu.backoffice.interfaces.dto;

import jakarta.validation.constraints.NotNull;
import id.payu.backoffice.domain.CustomerCaseStatus;

public record CustomerCaseUpdateRequest(
        @NotNull(message = "Status is required")
        CustomerCaseStatus status,

        String notes
) {}
