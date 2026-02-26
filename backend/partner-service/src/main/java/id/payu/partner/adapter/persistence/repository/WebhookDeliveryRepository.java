package id.payu.partner.adapter.persistence.repository;

import id.payu.partner.domain.WebhookDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

    Page<WebhookDelivery> findBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId, Pageable pageable);

    /**
     * Find deliveries that are ready for retry (FAILED status, next_retry_at has passed).
     */
    @Query("SELECT wd FROM WebhookDelivery wd " +
           "WHERE wd.status = 'FAILED' " +
           "AND wd.nextRetryAt <= :now " +
           "ORDER BY wd.nextRetryAt ASC")
    List<WebhookDelivery> findRetryableDeliveries(@Param("now") LocalDateTime now);

    /**
     * Find pending deliveries (never attempted yet).
     */
    List<WebhookDelivery> findByStatusOrderByCreatedAtAsc(WebhookDelivery.Status status);

    /**
     * Count deliveries by status for a subscription (for dashboard).
     */
    long countBySubscriptionIdAndStatus(Long subscriptionId, WebhookDelivery.Status status);

    /**
     * Clean up old delivered/exhausted records (retention policy).
     */
    @Modifying
    @Query("DELETE FROM WebhookDelivery wd " +
           "WHERE wd.status IN ('DELIVERED', 'EXHAUSTED') " +
           "AND wd.createdAt < :cutoff")
    int deleteOldDeliveries(@Param("cutoff") LocalDateTime cutoff);
}
