package id.payu.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubscribeRequest(
        @NotBlank(message = "Account ID is required")
        String accountId,

        @NotNull(message = "Plan ID is required")
        UUID planId,

        String externalReferenceId
) {
}
