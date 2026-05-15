package id.payu.cms.domain.port.out;

import id.payu.cms.adapter.persistence.entity.ContentEntity;
import id.payu.cms.domain.entity.ContentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for Content persistence.
 */
public interface ContentPersistencePort {

    ContentEntity save(ContentEntity content);

    Optional<ContentEntity> findById(UUID id);

    List<ContentEntity> findAll(int page, int size, String sortBy, String sortDirection);

    long count();

    List<ContentEntity> findByContentType(String contentType);

    List<ContentEntity> findByStatus(ContentStatus status);

    List<ContentEntity> findActiveByContentType(String contentType, LocalDate currentDate);

    boolean existsByTitleIgnoreCase(String title);

    boolean existsById(UUID id);

    List<ContentEntity> findScheduledToActivate(LocalDate currentDate);

    List<ContentEntity> findActiveToArchive(LocalDate currentDate);

    void deleteById(UUID id);
}
