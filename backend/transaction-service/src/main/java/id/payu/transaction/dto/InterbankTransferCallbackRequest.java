package id.payu.transaction.dto;

import jakarta.validation.constraints.NotBlank;

public record InterbankTransferCallbackRequest(
        @NotBlank String referenceNumber,
        @NotBlank String status,
        String failureReason
) {
}
