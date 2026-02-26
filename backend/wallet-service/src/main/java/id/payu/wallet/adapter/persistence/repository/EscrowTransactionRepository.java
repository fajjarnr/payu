package id.payu.wallet.adapter.persistence.repository;

import id.payu.wallet.adapter.persistence.entity.EscrowTransactionEntity;
import id.payu.wallet.adapter.persistence.entity.EscrowTransactionEntity.EscrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for escrow transactions.
 */
@Repository
public interface EscrowTransactionRepository extends JpaRepository<EscrowTransactionEntity, UUID> {

    List<EscrowTransactionEntity> findByBuyerAccountIdOrderByCreatedAtDesc(String buyerAccountId);

    List<EscrowTransactionEntity> findBySellerAccountIdOrderByCreatedAtDesc(String sellerAccountId);

    List<EscrowTransactionEntity> findByPartnerIdOrderByCreatedAtDesc(String partnerId);

    Optional<EscrowTransactionEntity> findByExternalReferenceId(String externalReferenceId);

    List<EscrowTransactionEntity> findByStatus(EscrowStatus status);

    /**
     * Find escrows that have expired and are still in HELD status — candidates for auto-refund.
     */
    @Query("SELECT e FROM EscrowTransactionEntity e WHERE e.status = 'HELD' AND e.expiresAt < :now")
    List<EscrowTransactionEntity> findExpiredHeldEscrows(@Param("now") LocalDateTime now);

    /**
     * Find escrows by buyer and status.
     */
    List<EscrowTransactionEntity> findByBuyerAccountIdAndStatus(String buyerAccountId, EscrowStatus status);

    /**
     * Find escrows by seller and status.
     */
    List<EscrowTransactionEntity> findBySellerAccountIdAndStatus(String sellerAccountId, EscrowStatus status);
}
