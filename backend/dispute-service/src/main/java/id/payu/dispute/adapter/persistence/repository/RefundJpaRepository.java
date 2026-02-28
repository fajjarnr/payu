package id.payu.dispute.adapter.persistence.repository;

import id.payu.dispute.adapter.persistence.entity.RefundEntity;
import id.payu.dispute.domain.model.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA Repository for RefundEntity.
 */
@Repository
public interface RefundJpaRepository extends JpaRepository<RefundEntity, UUID> {

    /**
     * Finds all refunds for a transaction.
     *
     * @param transactionId the transaction ID
     * @return list of refund entities
     */
    List<RefundEntity> findByTransactionId(UUID transactionId);

    /**
     * Finds refunds by status.
     *
     * @param status the refund status
     * @return list of refund entities
     */
    List<RefundEntity> findByStatus(RefundStatus status);
}
