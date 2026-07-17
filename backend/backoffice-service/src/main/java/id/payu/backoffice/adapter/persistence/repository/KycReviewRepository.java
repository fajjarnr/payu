package id.payu.backoffice.adapter.persistence.repository;

import id.payu.backoffice.adapter.persistence.entity.KycReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;
import java.util.UUID;
import id.payu.backoffice.domain.KycStatus;

@Repository
public interface KycReviewRepository extends JpaRepository<KycReviewEntity, UUID> {
    List<KycReviewEntity> findByStatus(KycStatus status);
    Page<KycReviewEntity> findByStatus(KycStatus status, Pageable pageable);
    List<KycReviewEntity> findByUserIdBlindIndexInOrderByCreatedAtDesc(Collection<String> userIdBlindIndexes);
    Page<KycReviewEntity> findByUserIdBlindIndexIsNull(Pageable pageable);

    @Query(value = """
            SELECT * FROM kyc_reviews
            WHERE user_id_blind_index IS NULL
               OR user_id_blind_index_key_version IS DISTINCT FROM :currentVersion
            ORDER BY created_at, id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<KycReviewEntity> lockNextPiiMigrationBatch(
            @Param("currentVersion") String currentVersion, @Param("batchSize") int batchSize);

}
