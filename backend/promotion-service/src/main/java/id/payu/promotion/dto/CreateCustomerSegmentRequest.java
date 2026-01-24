package id.payu.promotion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a new customer segment.
 */
public record CreateCustomerSegmentRequest(
    @NotBlank(message = "Segment name is required")
    String name,

    String description,

    @NotBlank(message = "Segment rules are required")
    String rules,

    @NotNull(message = "Active status is required")
    Boolean isActive,

    Integer priority
) {
}
