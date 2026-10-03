package id.payu.dispute.adapter.persistence.repository;

import id.payu.dispute.adapter.persistence.entity.DisputeEntity;
import id.payu.dispute.domain.model.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for DisputeEntity.
 */
@Repository
public interface DisputeJpaRepository extends JpaRepository<DisputeEntity, UUID> {

    List<DisputeEntity> findByTransactionId(UUID transactionId);

    Optional<DisputeEntity> findByIdAndCustomerId(UUID id, UUID customerId);

    List<DisputeEntity> findByTransactionIdAndCustomerId(UUID transactionId, UUID customerId);

    List<DisputeEntity> findByCustomerId(UUID customerId);

    List<DisputeEntity> findByMerchantId(UUID merchantId);

    List<DisputeEntity> findByStatus(DisputeStatus status);
}
