package id.payu.cms.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * DTO for creating/updating content
 */
@Schema(description = "Request DTO for creating or updating content")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentRequest {

    @Schema(
        description = "Content type",
        example = "BANNER",
        requiredMode = Schema.RequiredMode.REQUIRED,
        allowableValues = {"BANNER", "PROMO", "ALERT", "POPUP"}
    )
    @NotBlank(message = "Content type is required")
    @Size(max = 50, message = "Content type must not exceed 50 characters")
    private String contentType;

    @Schema(
        description = "Content title",
        example = "Special Promo January 2026",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Schema(
        description = "Content description",
        example = "Get 20% cashback on all transactions"
    )
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Schema(
        description = "Image URL for banner/promo",
        example = "https://cdn.payu.id/images/promo-jan2026.png"
    )
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    @Schema(
        description = "Action URL when content is clicked",
        example = "https://payu.id/promos/january-2026"
    )
    @Size(max = 500, message = "Action URL must not exceed 500 characters")
    private String actionUrl;

    @Schema(
        description = "Action type",
        example = "LINK",
        allowableValues = {"LINK", "DEEP_LINK", "DISMISS"}
    )
    @Size(max = 50, message = "Action type must not exceed 50 characters")
    private String actionType;

    @Schema(
        description = "Start date for content visibility",
        example = "2026-01-25"
    )
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(
        description = "End date for content visibility",
        example = "2026-01-31"
    )
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Schema(
        description = "Priority (higher = shown first)",
        example = "100"
    )
    private Integer priority;

    @Schema(
        description = "Targeting rules for user segmentation",
        example = "{\"segment\": \"PREMIUM\", \"location\": \"JAKARTA\", \"device\": \"MOBILE\"}"
    )
    private Map<String, Object> targetingRules;

    @Schema(
        description = "Additional metadata",
        example = "{\"abTest\": \"A\", \"campaign\": \"JAN2026\"}"
    )
    private Map<String, Object> metadata;
}
