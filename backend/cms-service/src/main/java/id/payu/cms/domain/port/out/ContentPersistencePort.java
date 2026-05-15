package id.payu.cms.domain.port.out;

import id.payu.cms.domain.entity.Content;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for Content persistence.
 */
public interface ContentPersistencePort {

    Content save(Content content);

    Optional<Content> findById(UUID id);

    List<Content> findAll(int page, int size, String sortBy, String sortDirection);

    long count();

    List<Content> findByContentType(String contentType);

    List<Content> findByStatus(Content.ContentStatus status);

    List<Content> findActiveByContentType(String contentType, LocalDate currentDate);

    boolean existsByTitleIgnoreCase(String title);

    boolean existsById(UUID id);

    List<Content> findScheduledToActivate(LocalDate currentDate);

    List<Content> findActiveToArchive(LocalDate currentDate);

    void deleteById(UUID id);
}
