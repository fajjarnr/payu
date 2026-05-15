package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.adapter.persistence.entity.BatchDisbursementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for BatchDisbursementEntity entity.
 */
@Repository
public interface BatchDisbursementJpaRepository extends JpaRepository<BatchDisbursementEntity, UUID> {

    Optional<BatchDisbursementEntity> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT b FROM BatchDisbursementEntity b WHERE b.sourceAccountId = :accountId ORDER BY b.createdAt DESC")
    List<BatchDisbursementEntity> findBySourceAccountId(@Param("accountId") UUID sourceAccountId,
                                                   org.springframework.data.domain.Pageable pageable);

    @Query("SELECT b FROM BatchDisbursementEntity b WHERE b.status = :status ORDER BY b.createdAt DESC")
    List<BatchDisbursementEntity> findByStatus(@Param("status") String status,
                                          org.springframework.data.domain.Pageable pageable);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
