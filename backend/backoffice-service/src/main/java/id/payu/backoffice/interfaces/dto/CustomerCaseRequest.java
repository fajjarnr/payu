package id.payu.backoffice.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import id.payu.backoffice.domain.CaseType;
import id.payu.backoffice.domain.Priority;

public record CustomerCaseRequest(
        @NotBlank(message = "User ID is required")
        String userId,

        String accountNumber,

        @NotNull(message = "Case type is required")
        CaseType caseType,

        Priority priority,

        @NotBlank(message = "Subject is required")
        String subject,

        String description,

        String notes
) {}
