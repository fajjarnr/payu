package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.adapter.persistence.entity.DisbursementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for DisbursementEntity entity.
 */
@Repository
public interface DisbursementJpaRepository extends JpaRepository<DisbursementEntity, UUID> {

    Optional<DisbursementEntity> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT d FROM DisbursementEntity d WHERE d.sourceAccountId = :accountId ORDER BY d.createdAt DESC")
    List<DisbursementEntity> findBySourceAccountId(@Param("accountId") UUID sourceAccountId,
                                              org.springframework.data.domain.Pageable pageable);

    @Query("SELECT d FROM DisbursementEntity d WHERE d.status = :status ORDER BY d.createdAt DESC")
    List<DisbursementEntity> findByStatus(@Param("status") String status,
                                     org.springframework.data.domain.Pageable pageable);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
