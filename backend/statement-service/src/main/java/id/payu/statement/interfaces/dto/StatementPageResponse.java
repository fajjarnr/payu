package id.payu.statement.interfaces.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable paged-list contract for the statement listing endpoint.
 *
 * <p>Spring Data {@code PageImpl} must never be serialized directly by a controller: it is
 * not a supported wire contract (logs "Serializing PageImpl instances as-is is not
 * supported") and its JSON shape is not guaranteed across versions. {@code PagedModel}
 * (VIA_DTO) is not an option either — the web client reads exactly these fields
 * (see {@code StatementsListResponse} in the web app), so keep them in sync.
 */
public record StatementPageResponse(
        List<StatementResponse> content,
        long totalElements,
        int totalPages,
        int size,
        int number,
        boolean first,
        boolean last
) {

    public static StatementPageResponse from(Page<StatementResponse> page) {
        return new StatementPageResponse(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getSize(),
                page.getNumber(),
                page.isFirst(),
                page.isLast()
        );
    }
}
