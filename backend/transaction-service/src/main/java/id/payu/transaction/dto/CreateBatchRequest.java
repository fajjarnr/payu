package id.payu.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a batch disbursement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBatchRequest {

    @NotNull(message = "Source account ID is required")
    private UUID sourceAccountId;

    @NotBlank(message = "Batch name is required")
    private String name;

    private String description;

    private String idempotencyKey;

    private List<BatchItemRequest> items;
}
