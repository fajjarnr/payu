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

    /**
     * Finds all disputes for a transaction.
     *
     * @param transactionId the transaction ID
     * @return list of dispute entities
     */
    List<DisputeEntity> findByTransactionId(UUID transactionId);

    Optional<DisputeEntity> findByIdAndCustomerId(UUID id, UUID customerId);

    List<DisputeEntity> findByTransactionIdAndCustomerId(UUID transactionId, UUID customerId);

    /**
     * Finds disputes by customer.
     *
     * @param customerId the customer ID
     * @return list of dispute entities
     */
    List<DisputeEntity> findByCustomerId(UUID customerId);

    /**
     * Finds disputes by merchant.
     *
     * @param merchantId the merchant ID
     * @return list of dispute entities
     */
    List<DisputeEntity> findByMerchantId(UUID merchantId);

    /**
     * Finds disputes by status.
     *
     * @param status the dispute status
     * @return list of dispute entities
     */
    List<DisputeEntity> findByStatus(DisputeStatus status);
}
