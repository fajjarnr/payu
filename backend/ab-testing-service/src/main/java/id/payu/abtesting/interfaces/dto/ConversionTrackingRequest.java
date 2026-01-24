package id.payu.abtesting.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for conversion tracking request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionTrackingRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Variant is required")
    private String variant; // CONTROL or VARIANT_B

    @NotBlank(message = "Event type is required")
    private String eventType; // conversion, participation, etc.
}
