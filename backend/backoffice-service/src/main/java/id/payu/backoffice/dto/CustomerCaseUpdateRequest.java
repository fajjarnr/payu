package id.payu.backoffice.dto;

import jakarta.validation.constraints.NotNull;
import id.payu.backoffice.adapter.persistence.entity.CustomerCaseEntity;
import id.payu.backoffice.domain.CustomerCaseStatus;

public record CustomerCaseUpdateRequest(
        @NotNull(message = "Status is required")
        CustomerCaseStatus status,

        String notes
) {}
