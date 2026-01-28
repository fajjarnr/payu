package id.payu.api.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Pagination information for list responses.
 * Provides metadata about paginated results and navigation links.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Pagination information for list responses")
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

    /**
     * Creates PaginationInfo from Spring Data Page.
     */
    public static <T> PaginationInfo from(org.springframework.data.domain.Page<T> page, String baseUrl) {
        int currentPage = page.getNumber();
        int pageSize = page.getSize();
        long totalElements = page.getTotalElements();
        int totalPages = page.getTotalPages();

        return PaginationInfo.builder()
                .page(currentPage)
                .size(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .links(PaginationLinks.builder()
                        .self(buildPageUrl(baseUrl, currentPage, pageSize))
                        .first(buildPageUrl(baseUrl, 0, pageSize))
                        .last(buildPageUrl(baseUrl, totalPages > 0 ? totalPages - 1 : 0, pageSize))
                        .next(page.hasNext() ? buildPageUrl(baseUrl, currentPage + 1, pageSize) : null)
                        .prev(page.hasPrevious() ? buildPageUrl(baseUrl, currentPage - 1, pageSize) : null)
                        .build())
                .build();
    }

    private static String buildPageUrl(String baseUrl, int page, int size) {
        return String.format("%s?page=%d&size=%d", baseUrl, page, size);
    }

    /**
     * Creates PaginationInfo from Page without links.
     */
    public static <T> PaginationInfo from(org.springframework.data.domain.Page<T> page) {
        return PaginationInfo.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
