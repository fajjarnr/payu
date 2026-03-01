package id.payu.statement.adapter.persistence.repository;

import id.payu.statement.adapter.persistence.entity.ReceiptEntity;
import id.payu.statement.domain.model.ReceiptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Receipt entity.
 * Epic E-19: Transaction Proof & Receipts (IMP-055)
 */
@Repository
public interface ReceiptJpaRepository extends JpaRepository<ReceiptEntity, UUID> {

    /**
     * Find receipt by transaction ID.
     */
    Optional<ReceiptEntity> findByTransactionId(String transactionId);

    /**
     * Check if receipt exists for transaction.
     */
    boolean existsByTransactionId(String transactionId);

    /**
     * Find receipts by status.
     */
    List<ReceiptEntity> findByStatus(ReceiptStatus status);

    /**
     * Find expired receipts that need status update.
     */
    @Query("SELECT r FROM ReceiptEntity r WHERE r.status = 'GENERATED' AND r.expiryDate < :now")
    List<ReceiptEntity> findExpiredReceipts(@Param("now") LocalDateTime now);

    /**
     * Count receipts by status.
     */
    long countByStatus(ReceiptStatus status);
}
