package id.payu.backoffice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import id.payu.backoffice.domain.CustomerCase;

public record CustomerCaseRequest(
        @NotBlank(message = "User ID is required")
        String userId,

        String accountNumber,

        @NotNull(message = "Case type is required")
        CustomerCase.CaseType caseType,

        CustomerCase.Priority priority,

        @NotBlank(message = "Subject is required")
        String subject,

        String description,

        String notes
) {}
