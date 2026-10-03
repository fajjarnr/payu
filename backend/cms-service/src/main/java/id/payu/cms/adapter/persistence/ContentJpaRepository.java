package id.payu.cms.adapter.persistence;

import id.payu.cms.adapter.persistence.entity.ContentEntity;
import id.payu.cms.domain.entity.ContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Content entity. Adapter-layer concern.
 * Domain code should depend on {@link id.payu.cms.domain.port.out.ContentPersistencePort}
 * instead of this interface directly.
 *
 * <p>BUG-CMS-HEX-001 Fix (iter 45): Moved from {@code domain/repository/} to
 * {@code adapter/persistence/} — Spring Data JPA is an adapter concern, not domain.
 * Renamed to {@code ContentJpaRepository} to reflect the JPA-specific nature.</p>
 */
public interface ContentJpaRepository extends JpaRepository<ContentEntity, UUID> {

    @Query("SELECT c FROM ContentEntity c WHERE c.contentType = :contentType " +
           "AND c.status = 'ACTIVE' " +
           "AND (c.startDate IS NULL OR c.startDate <= :currentDate) " +
           "AND (c.endDate IS NULL OR c.endDate >= :currentDate) " +
           "ORDER BY c.priority DESC")
    List<ContentEntity> findActiveByContentType(
        @Param("contentType") String contentType,
        @Param("currentDate") LocalDate currentDate
    );

    List<ContentEntity> findByStatus(ContentStatus status);

    List<ContentEntity> findByContentType(String contentType);

    // BUG-BE-058: Pageable version to prevent OOM with thousands of content items
    Page<ContentEntity> findByContentType(String contentType, Pageable pageable);

    Optional<ContentEntity> findByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCase(String title);

    @Query("SELECT c FROM ContentEntity c WHERE c.status = 'SCHEDULED' " +
           "AND c.startDate <= :currentDate")
    List<ContentEntity> findScheduledToActivate(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT c FROM ContentEntity c WHERE c.status = 'ACTIVE' " +
           "AND c.endDate IS NOT NULL AND c.endDate < :currentDate")
    List<ContentEntity> findActiveToArchive(@Param("currentDate") LocalDate currentDate);

    List<ContentEntity> findByCreatedBy(String createdBy);

    void deleteByStatus(ContentStatus status);
}
