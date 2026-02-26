package id.payu.partner.adapter.persistence.repository;

import id.payu.partner.domain.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, Long> {

    List<WebhookSubscription> findByPartnerId(Long partnerId);

    List<WebhookSubscription> findByPartnerIdAndActiveTrue(Long partnerId);

    /**
     * Find all active subscriptions that match a given event type.
     * Matches subscriptions with wildcard "*" or containing the event type in their comma-separated list.
     */
    @Query("SELECT ws FROM WebhookSubscription ws " +
           "WHERE ws.active = true " +
           "AND (ws.events = '*' OR ws.events LIKE CONCAT('%', :eventType, '%'))")
    List<WebhookSubscription> findActiveByEventType(@Param("eventType") String eventType);

    /**
     * Find active subscriptions for a specific partner that match a given event type.
     */
    @Query("SELECT ws FROM WebhookSubscription ws " +
           "WHERE ws.partner.id = :partnerId " +
           "AND ws.active = true " +
           "AND (ws.events = '*' OR ws.events LIKE CONCAT('%', :eventType, '%'))")
    List<WebhookSubscription> findActiveByPartnerIdAndEventType(
            @Param("partnerId") Long partnerId,
            @Param("eventType") String eventType);

    boolean existsByPartnerIdAndUrl(Long partnerId, String url);
}
