package id.payu.promotion.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating an existing customer segment.
 */
public record UpdateCustomerSegmentRequest(
    @NotBlank(message = "Segment name is required")
    String name,

    String description,

    @NotBlank(message = "Segment rules are required")
    String rules,

    Boolean isActive,

    Integer priority
) {
}
