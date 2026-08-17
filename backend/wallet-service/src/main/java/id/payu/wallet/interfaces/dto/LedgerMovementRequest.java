package id.payu.wallet.interfaces.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * PARTNER-PROD-005: batch lookup of ledger movements by external reference IDs
 * (SNAP payment references / refund UUIDs) for cross-service reconciliation.
 */
public record LedgerMovementRequest(
        @NotEmpty(message = "referenceIds must not be empty")
        @Size(max = 500, message = "referenceIds must not exceed 500 entries")
        List<String> referenceIds
) {
}
