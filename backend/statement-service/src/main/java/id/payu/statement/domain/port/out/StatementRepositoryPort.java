package id.payu.statement.domain.port.out;

import id.payu.statement.domain.entity.StatementStatus;
import id.payu.statement.domain.model.Statement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Statement persistence.
 */
public interface StatementRepositoryPort {

    boolean existsByCustomerIdAndStatementPeriod(String customerId, LocalDate statementPeriod);

    Statement save(Statement statement);

    Optional<Statement> findById(UUID id);

    Optional<Statement> findByIdAndCustomerId(UUID id, String customerId);

    Optional<Statement> findByCustomerIdAndStatementPeriod(String customerId, LocalDate statementPeriod);

    Page<Statement> findAllByCustomerId(String customerId, Pageable pageable);

    Optional<Statement> findLatestCompletedByCustomerId(String customerId);

    List<Statement> findByCustomerIdAndStatementPeriodBetween(String customerId, LocalDate startDate, LocalDate endDate);

    List<Statement> findStatementsForArchival(LocalDate cutoffDate);

    List<Statement> findStaleGeneratingStatements(LocalDateTime staleTime);

    long countByCustomerId(String customerId);

    List<Statement> findByStatus(StatementStatus status);
}
