package id.payu.statement.domain.repository;

import id.payu.statement.domain.entity.Statement;
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

/**
 * Extended Repository for Statement entity
 */
@Repository
public interface StatementRepository extends JpaRepository<Statement, UUID> {

    /**
     * Find all statements for a user with pagination
     */
    @Query("SELECT s FROM Statement s WHERE s.userId = :userId ORDER BY s.statementPeriod DESC")
    Page<Statement> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find statement by user and period
     */
    Optional<Statement> findByUserIdAndStatementPeriod(UUID userId, LocalDate statementPeriod);

    /**
     * Check if statement exists for user and period
     */
    boolean existsByUserIdAndStatementPeriod(UUID userId, LocalDate statementPeriod);

    /**
     * Find statements by status
     */
    List<Statement> findByStatus(Statement.StatementStatus status);

    /**
     * Find statement by ID and user ID (security check)
     */
    Optional<Statement> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Get latest completed statement for user
     */
    @Query("SELECT s FROM Statement s WHERE s.userId = :userId AND s.status = 'COMPLETED' ORDER BY s.statementPeriod DESC LIMIT 1")
    Optional<Statement> findLatestCompletedByUserId(@Param("userId") UUID userId);

    /**
     * Count statements by user
     */
    long countByUserId(UUID userId);

    /**
     * Find statements needing archival (older than 24 months)
     */
    @Query("SELECT s FROM Statement s WHERE s.statementPeriod < :cutoffDate AND s.status = 'COMPLETED'")
    List<Statement> findStatementsForArchival(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Find statements by date range for user
     */
    @Query("SELECT s FROM Statement s WHERE s.userId = :userId AND s.statementPeriod BETWEEN :startDate AND :endDate ORDER BY s.statementPeriod DESC")
    List<Statement> findByUserIdAndStatementPeriodBetween(UUID userId, LocalDate startDate, LocalDate endDate);

    /**
     * Find statements in generating status that may be stuck
     */
    @Query("SELECT s FROM Statement s WHERE s.status = 'GENERATING' AND s.createdAt < :staleTime")
    List<Statement> findStaleGeneratingStatements(@Param("staleTime") java.time.LocalDateTime staleTime);
}
