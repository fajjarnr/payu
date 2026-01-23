package id.payu.backoffice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

public record UniversalSearchRequest(
        String query,
        String entityType,
        List<String> fields,
        @Min(0) @Max(100) int page,
        @Min(1) @Max(100) int size
) {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    public UniversalSearchRequest {
        if (page < 0) {
            page = 0;
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            size = DEFAULT_PAGE_SIZE;
        }
    }

    public UniversalSearchRequest(String queryParam) {
        this(queryParam, null, null, 0, DEFAULT_PAGE_SIZE);
    }
}
