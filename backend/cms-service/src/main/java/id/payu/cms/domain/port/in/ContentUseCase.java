package id.payu.cms.domain.port.in;

import id.payu.cms.domain.dto.ContentListResponse;
import id.payu.cms.domain.dto.ContentRequest;
import id.payu.cms.domain.dto.ContentResponse;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port for Content management use cases.
 */
public interface ContentUseCase {

    ContentResponse createContent(ContentRequest request, String createdBy);

    ContentResponse updateContent(UUID id, ContentRequest request, String updatedBy);

    ContentResponse getContentById(UUID id);

    ContentListResponse getAllContent(int page, int size, String sortBy, String sortDirection);

    List<ContentResponse> getContentByType(String type);

    List<ContentResponse> getContentByStatus(String status);

    List<ContentResponse> getActiveContentByType(String type);

    ContentResponse updateContentStatus(UUID id, String status, String updatedBy);

    void deleteContent(UUID id);

    void activateScheduledContent(List<UUID> contentIds);

    void archiveExpiredContent(List<UUID> contentIds);
}
