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

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getVariant() { return variant; }
    public void setVariant(String variant) { this.variant = variant; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
}
