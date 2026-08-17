package id.payu.transaction.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record InterbankTransferCallbackRequest(
        @NotBlank String referenceNumber,
        @NotBlank String status,
        String failureReason
) {
}
