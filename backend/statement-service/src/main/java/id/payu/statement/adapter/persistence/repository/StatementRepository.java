package id.payu.statement.adapter.persistence.repository;

import id.payu.statement.adapter.persistence.entity.StatementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.statement.domain.entity.StatementStatus;

/**
 * Extended Repository for StatementEntity entity
 */
@Repository
public interface StatementRepository extends JpaRepository<StatementEntity, UUID> {

    /**
     * Find all statements for a customer with pagination
     */
    @Query("SELECT s FROM StatementEntity s WHERE s.customerId = :customerId ORDER BY s.statementPeriod DESC")
    Page<StatementEntity> findAllByCustomerId(@Param("customerId") String customerId, Pageable pageable);

    /**
     * Find statement by customer and period
     */
    Optional<StatementEntity> findByCustomerIdAndStatementPeriod(String customerId, LocalDate statementPeriod);

    /**
     * Check if statement exists for customer and period
     */
    boolean existsByCustomerIdAndStatementPeriod(String customerId, LocalDate statementPeriod);

    /**
     * Find statements by status
     */
    List<StatementEntity> findByStatus(StatementStatus status);

    /**
     * Find statement by ID and customer ID (security check)
     */
    Optional<StatementEntity> findByIdAndCustomerId(UUID id, String customerId);

    /**
     * Get latest completed statement for customer
     */
    @Query("SELECT s FROM StatementEntity s WHERE s.customerId = :customerId AND s.status = 'COMPLETED' ORDER BY s.statementPeriod DESC LIMIT 1")
    Optional<StatementEntity> findLatestCompletedByCustomerId(@Param("customerId") String customerId);

    /**
     * Count statements by customer
     */
    long countByCustomerId(String customerId);

    /**
     * Find statements needing archival (older than 24 months)
     */
    @Query("SELECT s FROM StatementEntity s WHERE s.statementPeriod < :cutoffDate AND s.status = 'COMPLETED'")
    List<StatementEntity> findStatementsForArchival(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Find statements by date range for customer
     */
    @Query("SELECT s FROM StatementEntity s WHERE s.customerId = :customerId AND s.statementPeriod BETWEEN :startDate AND :endDate ORDER BY s.statementPeriod DESC")
    List<StatementEntity> findByCustomerIdAndStatementPeriodBetween(String customerId, LocalDate startDate, LocalDate endDate);

    /**
     * Find statements in generating status that may be stuck
     */
    @Query("SELECT s FROM StatementEntity s WHERE s.status = 'GENERATING' AND s.createdAt < :staleTime")
    List<StatementEntity> findStaleGeneratingStatements(@Param("staleTime") java.time.LocalDateTime staleTime);
}
