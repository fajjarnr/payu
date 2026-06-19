package id.payu.cms.adapter.persistence;

import id.payu.cms.adapter.persistence.entity.ContentEntity;
import id.payu.cms.domain.entity.ContentStatus;
import id.payu.cms.domain.port.out.ContentPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementation of {@link ContentPersistencePort} using Spring Data JPA.
 *
 * <p>This is the boundary between the domain layer (which knows nothing about JPA)
 * and the persistence adapter (which translates domain operations into JPA calls).</p>
 *
 * <p>BUG-CMS-HEX-001 Fix (iter 45): Created so that
 * {@link id.payu.cms.application.service.ContentService} can depend on the port
 * interface instead of Spring Data JPA directly.</p>
 */
@Component
@RequiredArgsConstructor
public class ContentPersistenceAdapter implements ContentPersistencePort {

    private final ContentJpaRepository repository;

    @Override
    public ContentEntity save(ContentEntity content) {
        return repository.save(content);
    }

    @Override
    public Optional<ContentEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<ContentEntity> findAll(int page, int size, String sortBy, String sortDirection) {
        // Domain doesn't expose Pageable yet — adapter translates here
        var pageable = org.springframework.data.domain.PageRequest.of(
                page, size,
                "desc".equalsIgnoreCase(sortDirection)
                        ? org.springframework.data.domain.Sort.by(sortBy).descending()
                        : org.springframework.data.domain.Sort.by(sortBy).ascending()
        );
        return repository.findAll(pageable).getContent();
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public List<ContentEntity> findByContentType(String contentType) {
        return repository.findByContentType(contentType);
    }

    @Override
    public List<ContentEntity> findByStatus(ContentStatus status) {
        return repository.findByStatus(status);
    }

    @Override
    public List<ContentEntity> findActiveByContentType(String contentType, LocalDate currentDate) {
        return repository.findActiveByContentType(contentType, currentDate);
    }

    @Override
    public boolean existsByTitleIgnoreCase(String title) {
        return repository.existsByTitleIgnoreCase(title);
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public List<ContentEntity> findScheduledToActivate(LocalDate currentDate) {
        return repository.findScheduledToActivate(currentDate);
    }

    @Override
    public List<ContentEntity> findActiveToArchive(LocalDate currentDate) {
        return repository.findActiveToArchive(currentDate);
    }

    @Override
    public List<ContentEntity> findByCreatedBy(String createdBy) {
        return repository.findByCreatedBy(createdBy);
    }

    @Override
    public void deleteByStatus(ContentStatus status) {
        repository.deleteByStatus(status);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
