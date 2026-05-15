package id.payu.backoffice.domain.port.in;

import id.payu.backoffice.dto.UniversalSearchResponse;

/**
 * Inbound port for Universal Search use cases.
 */
public interface UniversalSearchUseCase {

    UniversalSearchResponse search(String query, String entityType, int page, int size);
}
