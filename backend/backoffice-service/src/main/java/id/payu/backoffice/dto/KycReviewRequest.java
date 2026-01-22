package id.payu.backoffice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record KycReviewRequest(
        @NotBlank(message = "User ID is required")
        String userId,

        @NotBlank(message = "Account number is required")
        String accountNumber,

        String documentType,

        String documentNumber,

        String documentUrl,

        String fullName,

        String address,

        String phoneNumber,

        String notes
) {}
