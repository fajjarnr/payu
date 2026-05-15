package id.payu.compliance.adapter.persistence.repository;

import id.payu.compliance.adapter.persistence.entity.DataAccessAuditEntity;
import id.payu.compliance.domain.model.DataOperationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DataAccessAuditRepository extends JpaRepository<DataAccessAuditEntity, UUID> {

    Page<DataAccessAuditEntity> findByUserIdOrderByAccessedAtDesc(String userId, Pageable pageable);

    List<DataAccessAuditEntity> findByUserIdAndAccessedAtBetweenOrderByAccessedAtDesc(
            String userId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    List<DataAccessAuditEntity> findByAccessedByAndAccessedAtBetweenOrderByAccessedAtDesc(
            String accessedBy,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    Page<DataAccessAuditEntity> findByOperationTypeOrderByAccessedAtDesc(
            DataOperationType operationType,
            Pageable pageable
    );

    @Query("SELECT da FROM DataAccessAuditEntity da WHERE da.serviceName = :serviceName AND da.accessedAt BETWEEN :startDate AND :endDate ORDER BY da.accessedAt DESC")
    List<DataAccessAuditEntity> findByServiceNameAndDateRange(
            @Param("serviceName") String serviceName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(da) FROM DataAccessAuditEntity da WHERE da.userId = :userId AND da.accessedAt >= :startDate")
    long countByUserIdSinceDate(
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate
    );

    @Query("SELECT da FROM DataAccessAuditEntity da WHERE da.success = false AND da.accessedAt >= :startDate")
    List<DataAccessAuditEntity> findFailedAccessAttemptsSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT da FROM DataAccessAuditEntity da WHERE " +
           "(:userId IS NULL OR da.userId = :userId) AND " +
           "(:accessedBy IS NULL OR da.accessedBy = :accessedBy) AND " +
           "(:serviceName IS NULL OR da.serviceName = :serviceName) AND " +
           "(:operationType IS NULL OR da.operationType = :operationType) AND " +
           "(:startDate IS NULL OR da.accessedAt >= :startDate) AND " +
           "(:endDate IS NULL OR da.accessedAt <= :endDate) " +
           "ORDER BY da.accessedAt DESC")
    Page<DataAccessAuditEntity> findByFilters(
            @Param("userId") String userId,
            @Param("accessedBy") String accessedBy,
            @Param("serviceName") String serviceName,
            @Param("operationType") DataOperationType operationType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}