package id.payu.cms.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for paginated content list response
 */
@Schema(description = "Response DTO for content list with pagination")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentListResponse {

    @Schema(description = "List of contents")
    private List<ContentResponse> contents;

    @Schema(description = "Current page number (0-based)", example = "0")
    private int page;

    @Schema(description = "Page size", example = "20")
    private int size;

    @Schema(description = "Total elements", example = "150")
    private long totalElements;

    @Schema(description = "Total pages", example = "8")
    private int totalPages;

    @Schema(description = "Is this the first page?", example = "true")
    private boolean first;

    @Schema(description = "Is this the last page?", example = "false")
    private boolean last;

    /**
     * Create empty response
     */
    public static ContentListResponse empty() {
        return ContentListResponse.builder()
            .contents(List.of())
            .page(0)
            .size(0)
            .totalElements(0)
            .totalPages(0)
            .first(true)
            .last(true)
            .build();
    }
}
