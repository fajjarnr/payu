package id.payu.dispute.adapter.persistence.repository;

import id.payu.dispute.adapter.persistence.entity.RefundEntity;
import id.payu.dispute.domain.model.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA Repository for RefundEntity.
 */
@Repository
public interface RefundJpaRepository extends JpaRepository<RefundEntity, UUID> {

    List<RefundEntity> findByTransactionId(UUID transactionId);

    List<RefundEntity> findByStatus(RefundStatus status);

    /**
     * DISPUTE-001: transaction-scoped advisory lock keyed by transaction id,
     * released on commit/rollback of the surrounding transaction.
     */
    @Query(value = "SELECT pg_advisory_xact_lock(hashtextextended(:key, 0))", nativeQuery = true)
    void lockTransaction(@Param("key") String transactionId);
}
