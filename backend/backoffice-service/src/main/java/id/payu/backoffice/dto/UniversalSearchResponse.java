package id.payu.backoffice.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UniversalSearchResponse(
        String query,
        int page,
        int size,
        long totalResults,
        List<SearchResultItem> results
) {
    public record SearchResultItem(
            String type,
            UUID id,
            String title,
            String description,
            String userId,
            String accountNumber,
            String status,
            LocalDateTime createdAt,
            Object details
    ) {
    }
}
