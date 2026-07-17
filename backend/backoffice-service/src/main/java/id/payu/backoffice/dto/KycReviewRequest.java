package id.payu.backoffice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

public record KycReviewRequest(
        @NotBlank(message = "User ID is required")
        @Pattern(regexp = "\\S(?:.*\\S)?", message = "User ID must not contain leading or trailing whitespace")
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
