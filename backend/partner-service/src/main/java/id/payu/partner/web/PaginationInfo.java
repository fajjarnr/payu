package id.payu.partner.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pagination information for list responses.
 * Provides metadata about paginated results and navigation links.
 */
@Schema(description = "Pagination information for list responses")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginationInfo {

    @Schema(description = "Current page number (0-based)", example = "0")
    private int page;

    @Schema(description = "Number of items per page", example = "20")
    private int size;

    @Schema(description = "Total number of elements", example = "150")
    private long totalElements;

    @Schema(description = "Total number of pages", example = "8")
    private int totalPages;

    @Schema(description = "Whether there is a next page", example = "true")
    private boolean hasNext;

    @Schema(description = "Whether there is a previous page", example = "false")
    private boolean hasPrevious;

    @Schema(description = "Navigation links for pagination")
    private PaginationLinks links;

    // Static factory methods for convenience
    public static PaginationInfo of(int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PaginationInfo(page, size, totalElements, totalPages,
            page < totalPages - 1, page > 0, null);
    }

    /**
     * Pagination links for navigation.
     */
    @Schema(description = "Navigation links for pagination")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationLinks {

        @Schema(description = "URL for the current page", example = "/partners?page=0&size=20")
        private String self;

        @Schema(description = "URL for the first page", example = "/partners?page=0&size=20")
        private String first;

        @Schema(description = "URL for the last page", example = "/partners?page=7&size=20")
        private String last;

        @Schema(description = "URL for the next page (if available)", example = "/partners?page=1&size=20")
        private String next;

        @Schema(description = "URL for the previous page (if available)", example = "/partners?page=0&size=20")
        private String prev;
    }
}
