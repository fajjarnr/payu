package id.payu.wallet.adapter.persistence.repository;

import id.payu.wallet.adapter.persistence.entity.WalletTransactionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

/**
 * Spring Data JPA repository for WalletTransactionEntity.
 */
@Repository
public interface WalletTransactionJpaRepository extends JpaRepository<WalletTransactionEntity, UUID> {

    List<WalletTransactionEntity> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);

    /**
     * Keyset cursor pagination for wallet transactions (ARCH-PAGE-001).
     * Avoids O(N) offset scan overhead on large transaction history datasets.
     */
    @org.springframework.data.jpa.repository.Query("SELECT wt FROM WalletTransactionEntity wt WHERE wt.walletId = :walletId " +
           "AND (wt.createdAt < :lastCreatedAt OR (wt.createdAt = :lastCreatedAt AND wt.id < :lastId)) " +
           "ORDER BY wt.createdAt DESC, wt.id DESC")
    List<WalletTransactionEntity> findByWalletIdKeyset(
            @org.springframework.data.repository.query.Param("walletId") UUID walletId,
            @org.springframework.data.repository.query.Param("lastCreatedAt") java.time.LocalDateTime lastCreatedAt,
            @org.springframework.data.repository.query.Param("lastId") UUID lastId,
            Pageable pageable);

    Optional<WalletTransactionEntity> findByReferenceId(String referenceId);
}
