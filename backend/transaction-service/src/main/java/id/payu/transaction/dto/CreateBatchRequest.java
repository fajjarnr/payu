package id.payu.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a batch disbursement.
 */
public class CreateBatchRequest {

    @NotNull(message = "Source account ID is required")
    private UUID sourceAccountId;

    @NotBlank(message = "Batch name is required")
    private String name;

    private String description;

    private String idempotencyKey;

    private List<BatchItemRequest> items;

    // Default constructor
    public CreateBatchRequest() {
    }

    // Getters and Setters
    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(UUID sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public List<BatchItemRequest> getItems() {
        return items;
    }

    public void setItems(List<BatchItemRequest> items) {
        this.items = items;
    }
}
