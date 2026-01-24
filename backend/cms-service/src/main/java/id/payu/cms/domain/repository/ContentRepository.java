package id.payu.cms.domain.repository;

import id.payu.cms.domain.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Content entity
 */
@Repository
public interface ContentRepository extends JpaRepository<Content, UUID> {

    /**
     * Find active content by type
     */
    @Query("SELECT c FROM Content c WHERE c.contentType = :contentType " +
           "AND c.status = 'ACTIVE' " +
           "AND (c.startDate IS NULL OR c.startDate <= :currentDate) " +
           "AND (c.endDate IS NULL OR c.endDate >= :currentDate) " +
           "ORDER BY c.priority DESC")
    List<Content> findActiveByContentType(
        @Param("contentType") String contentType,
        @Param("currentDate") LocalDate currentDate
    );

    /**
     * Find all content by status
     */
    List<Content> findByStatus(Content.ContentStatus status);

    /**
     * Find all content by type
     */
    List<Content> findByContentType(String contentType);

    /**
     * Find content by title (case-insensitive)
     */
    Optional<Content> findByTitleIgnoreCase(String title);

    /**
     * Check if content exists by title
     */
    boolean existsByTitleIgnoreCase(String title);

    /**
     * Find scheduled content that should be activated
     */
    @Query("SELECT c FROM Content c WHERE c.status = 'SCHEDULED' " +
           "AND c.startDate <= :currentDate")
    List<Content> findScheduledToActivate(@Param("currentDate") LocalDate currentDate);

    /**
     * Find active content that should be archived (past end date)
     */
    @Query("SELECT c FROM Content c WHERE c.status = 'ACTIVE' " +
           "AND c.endDate IS NOT NULL AND c.endDate < :currentDate")
    List<Content> findActiveToArchive(@Param("currentDate") LocalDate currentDate);

    /**
     * Find content by creator
     */
    List<Content> findByCreatedBy(String createdBy);

    /**
     * Delete content by status
     */
    void deleteByStatus(Content.ContentStatus status);
}
