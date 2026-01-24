package id.payu.cms.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for content response
 */
@Schema(description = "Response DTO for content")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentResponse {

    @Schema(description = "Content ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Content type", example = "BANNER")
    private String contentType;

    @Schema(description = "Content title", example = "Special Promo January 2026")
    private String title;

    @Schema(description = "Content description", example = "Get 20% cashback on all transactions")
    private String description;

    @Schema(description = "Image URL", example = "https://cdn.payu.id/images/promo-jan2026.png")
    private String imageUrl;

    @Schema(description = "Action URL", example = "https://payu.id/promos/january-2026")
    private String actionUrl;

    @Schema(description = "Action type", example = "LINK")
    private String actionType;

    @Schema(description = "Start date", example = "2026-01-25")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(description = "End date", example = "2026-01-31")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Schema(description = "Priority", example = "100")
    private Integer priority;

    @Schema(description = "Content status", example = "ACTIVE")
    private String status;

    @Schema(description = "Targeting rules")
    private Map<String, Object> targetingRules;

    @Schema(description = "Additional metadata")
    private Map<String, Object> metadata;

    @Schema(description = "Content version", example = "1")
    private Integer version;

    @Schema(description = "Creation timestamp", example = "2026-01-24T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2026-01-24T11:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @Schema(description = "Creator", example = "admin@payu.id")
    private String createdBy;

    @Schema(description = "Last updater", example = "admin@payu.id")
    private String updatedBy;

    @Schema(description = "Is currently active", example = "true")
    private boolean active;
}
