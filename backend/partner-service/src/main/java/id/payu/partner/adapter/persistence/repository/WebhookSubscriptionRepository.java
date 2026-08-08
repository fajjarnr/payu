package id.payu.partner.adapter.persistence.repository;

import id.payu.partner.adapter.persistence.entity.WebhookSubscriptionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscriptionEntity, Long> {

    List<WebhookSubscriptionEntity> findByPartnerId(Long partnerId);

    List<WebhookSubscriptionEntity> findByPartnerIdAndActiveTrue(Long partnerId);

    /**
     * Find all active subscriptions that match a given event type.
     * Matches subscriptions with wildcard "*" or containing the event type in their comma-separated list.
     */
    @Query("SELECT ws FROM WebhookSubscriptionEntity ws " +
           "WHERE ws.active = true " +
           "AND (ws.events = '*' OR ws.events LIKE CONCAT('%', :eventType, '%'))")
    List<WebhookSubscriptionEntity> findActiveByEventType(@Param("eventType") String eventType);

    /**
     * Find active subscriptions for a specific partner that match a given event type.
     */
    @Query("SELECT ws FROM WebhookSubscriptionEntity ws " +
           "WHERE ws.partner.id = :partnerId " +
           "AND ws.active = true " +
           "AND (ws.events = '*' OR ws.events LIKE CONCAT('%', :eventType, '%'))")
    List<WebhookSubscriptionEntity> findActiveByPartnerIdAndEventType(
            @Param("partnerId") Long partnerId,
            @Param("eventType") String eventType);

    boolean existsByPartnerIdAndUrl(Long partnerId, String url);

    /**
     * PARTNER-PROD-002: lock a batch of subscriptions whose webhook signing
     * secret is still legacy plaintext (not yet ENC(...) ciphertext) so the
     * scheduled backfill can re-save them through the entity and encrypt at rest.
     * <p>JPQL (not native) so the {@code EncryptedStringConverter} runs on read
     * and the write path sees a dirty change.
     */
    @Query(value = """
            SELECT ws FROM WebhookSubscriptionEntity ws
            WHERE ws.secret IS NOT NULL AND ws.secret <> '' AND ws.secret NOT LIKE 'ENC%'
            ORDER BY ws.id
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<WebhookSubscriptionEntity> lockNextPlaintextSecretBatch(Pageable pageable);

    /**
     * PARTNER-PROD-002: force-rewrite a legacy plaintext webhook secret through
     * the converter (bulk JPQL always executes SQL and binds through the converter).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE WebhookSubscriptionEntity ws SET ws.secret = :secret WHERE ws.id = :id")
    int rewriteEncryptedSecret(@Param("id") Long id, @Param("secret") String secret);
}
