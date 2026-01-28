package id.payu.api.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Navigation links for paginated responses.
 * Follows HATEOAS principles for API navigation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Navigation links for pagination")
public class PaginationLinks {

    @Schema(description = "URL of the current page", example = "/v1/transactions?page=0&size=20")
    private String self;

    @Schema(description = "URL of the first page", example = "/v1/transactions?page=0&size=20")
    private String first;

    @Schema(description = "URL of the last page", example = "/v1/transactions?page=7&size=20")
    private String last;

    @Schema(description = "URL of the next page (null if no next page)", example = "/v1/transactions?page=1&size=20")
    private String next;

    @Schema(description = "URL of the previous page (null if no previous page)", example = "/v1/transactions?page=0&size=20")
    private String prev;
}
