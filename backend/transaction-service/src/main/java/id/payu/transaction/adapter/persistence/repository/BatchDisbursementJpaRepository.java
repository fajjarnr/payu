package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.domain.model.BatchDisbursement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for BatchDisbursement entity.
 */
@Repository
public interface BatchDisbursementJpaRepository extends JpaRepository<BatchDisbursement, UUID> {

    Optional<BatchDisbursement> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT b FROM BatchDisbursement b WHERE b.sourceAccountId = :accountId ORDER BY b.createdAt DESC")
    List<BatchDisbursement> findBySourceAccountId(@Param("accountId") UUID sourceAccountId,
                                                   org.springframework.data.domain.Pageable pageable);

    @Query("SELECT b FROM BatchDisbursement b WHERE b.status = :status ORDER BY b.createdAt DESC")
    List<BatchDisbursement> findByStatus(@Param("status") String status,
                                          org.springframework.data.domain.Pageable pageable);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
