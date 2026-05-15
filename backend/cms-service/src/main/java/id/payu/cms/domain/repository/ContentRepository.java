package id.payu.cms.domain.repository;

import id.payu.cms.adapter.persistence.entity.ContentEntity;
import id.payu.cms.domain.entity.ContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Content entity
 */
@Repository
public interface ContentRepository extends JpaRepository<ContentEntity, UUID> {

    /**
     * Find active content by type
     */
    @Query("SELECT c FROM ContentEntity c WHERE c.contentType = :contentType " +
           "AND c.status = 'ACTIVE' " +
           "AND (c.startDate IS NULL OR c.startDate <= :currentDate) " +
           "AND (c.endDate IS NULL OR c.endDate >= :currentDate) " +
           "ORDER BY c.priority DESC")
    List<ContentEntity> findActiveByContentType(
        @Param("contentType") String contentType,
        @Param("currentDate") LocalDate currentDate
    );

    /**
     * Find all content by status
     */
    List<ContentEntity> findByStatus(ContentStatus status);

    /**
     * Find all content by type
     */
    List<ContentEntity> findByContentType(String contentType);

    // BUG-BE-058: Pageable version to prevent OOM with thousands of content items
    Page<ContentEntity> findByContentType(String contentType, Pageable pageable);

    /**
     * Find content by title (case-insensitive)
     */
    Optional<ContentEntity> findByTitleIgnoreCase(String title);

    /**
     * Check if content exists by title
     */
    boolean existsByTitleIgnoreCase(String title);

    /**
     * Find scheduled content that should be activated
     */
    @Query("SELECT c FROM ContentEntity c WHERE c.status = 'SCHEDULED' " +
           "AND c.startDate <= :currentDate")
    List<ContentEntity> findScheduledToActivate(@Param("currentDate") LocalDate currentDate);

    /**
     * Find active content that should be archived (past end date)
     */
    @Query("SELECT c FROM ContentEntity c WHERE c.status = 'ACTIVE' " +
           "AND c.endDate IS NOT NULL AND c.endDate < :currentDate")
    List<ContentEntity> findActiveToArchive(@Param("currentDate") LocalDate currentDate);

    /**
     * Find content by creator
     */
    List<ContentEntity> findByCreatedBy(String createdBy);

    /**
     * Delete content by status
     */
    void deleteByStatus(ContentStatus status);
}
