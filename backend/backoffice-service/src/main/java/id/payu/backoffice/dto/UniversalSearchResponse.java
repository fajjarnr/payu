package id.payu.backoffice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response wrapper for universal search results.
 * Contains search metadata and a list of matching items.
 */
@Schema(description = "Universal search response with paginated results")
public record UniversalSearchResponse(
        @Schema(description = "The search query string", example = "John Doe")
        String query,

        @Schema(description = "Current page number (0-based)", example = "0")
        int page,

        @Schema(description = "Number of items per page", example = "20")
        int size,

        @Schema(description = "Total number of matching results", example = "150")
        long totalResults,

        @Schema(description = "List of search results")
        List<SearchResultItem> results
) {
    /**
     * Individual search result item from universal search.
     */
    @Schema(description = "Single search result item")
    public record SearchResultItem(
            @Schema(description = "Entity type (kyc_review, fraud_case, customer_case)", example = "kyc_review")
            String type,

            @Schema(description = "Unique identifier of the entity", example = "123e4567-e89b-12d3-a456-426614174000")
            UUID id,

            @Schema(description = "Title or summary of the result", example = "KYC Review for John Doe")
            String title,

            @Schema(description = "Detailed description", example = "Pending KYC verification")
            String description,

            @Schema(description = "Associated user ID", example = "user-123")
            String userId,

            @Schema(description = "Associated account number", example = "1234567890")
            String accountNumber,

            @Schema(description = "Current status", example = "PENDING")
            String status,

            @Schema(description = "Creation timestamp", example = "2026-01-31T10:30:00")
            LocalDateTime createdAt,

            @Schema(description = "Additional entity-specific details")
            Object details
    ) {
    }
}
