package id.payu.integration.adapter.persistence.repository;

import id.payu.integration.adapter.persistence.entity.IntegrationMessageEntity;
import id.payu.integration.domain.model.MessageStatus;
import id.payu.integration.domain.model.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for IntegrationMessageEntity.
 */
@Repository
public interface IntegrationMessageJpaRepository extends JpaRepository<IntegrationMessageEntity, String> {

    List<IntegrationMessageEntity> findByStatus(MessageStatus status);

    List<IntegrationMessageEntity> findByType(MessageType type);

    List<IntegrationMessageEntity> findByCorrelationId(String correlationId);

    Optional<IntegrationMessageEntity> findByBusinessReference(String businessReference);

    List<IntegrationMessageEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT m FROM IntegrationMessageEntity m WHERE m.status = 'FAILED' AND m.retryCount < m.maxRetries")
    List<IntegrationMessageEntity> findRetryableMessages();

    long countByStatus(MessageStatus status);

    void deleteByCreatedAtBefore(LocalDateTime cutoff);

    @Query("SELECT m FROM IntegrationMessageEntity m WHERE m.status IN ('RECEIVED', 'VALIDATING', 'TRANSFORMING', 'SENDING') AND m.createdAt < :cutoff")
    List<IntegrationMessageEntity> findStaleMessages(@Param("cutoff") LocalDateTime cutoff);
}
