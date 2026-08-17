package id.payu.statement.adapter.persistence;

import id.payu.statement.adapter.persistence.entity.StatementEntity;
import id.payu.statement.adapter.persistence.repository.StatementRepository;
import id.payu.statement.domain.entity.StatementStatus;
import id.payu.statement.domain.model.Statement;
import id.payu.statement.domain.port.out.StatementRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementing StatementRepositoryPort using JPA repository.
 */
@Component
@RequiredArgsConstructor
public class StatementRepositoryAdapter implements StatementRepositoryPort {

    private final StatementRepository statementRepository;

    @Override
    public boolean existsByCustomerIdAndStatementPeriod(String customerId, LocalDate statementPeriod) {
        return statementRepository.existsByCustomerIdAndStatementPeriod(customerId, statementPeriod);
    }

    @Override
    public Statement save(Statement statement) {
        StatementEntity entity = toEntity(statement);
        StatementEntity saved = statementRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Statement> findById(UUID id) {
        return statementRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Statement> findByIdAndCustomerId(UUID id, String customerId) {
        return statementRepository.findByIdAndCustomerId(id, customerId).map(this::toDomain);
    }

    @Override
    public Optional<Statement> findByCustomerIdAndStatementPeriod(String customerId, LocalDate statementPeriod) {
        return statementRepository.findByCustomerIdAndStatementPeriod(customerId, statementPeriod).map(this::toDomain);
    }

    @Override
    public Page<Statement> findAllByCustomerId(String customerId, Pageable pageable) {
        return statementRepository.findAllByCustomerId(customerId, pageable).map(this::toDomain);
    }

    @Override
    public Optional<Statement> findLatestCompletedByCustomerId(String customerId) {
        return statementRepository.findLatestCompletedByCustomerId(customerId).map(this::toDomain);
    }

    @Override
    public List<Statement> findByCustomerIdAndStatementPeriodBetween(String customerId, LocalDate startDate, LocalDate endDate) {
        return statementRepository.findByCustomerIdAndStatementPeriodBetween(customerId, startDate, endDate)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Statement> findStatementsForArchival(LocalDate cutoffDate) {
        return statementRepository.findStatementsForArchival(cutoffDate)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Statement> findStaleGeneratingStatements(LocalDateTime staleTime) {
        return statementRepository.findStaleGeneratingStatements(staleTime)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long countByCustomerId(String customerId) {
        return statementRepository.countByCustomerId(customerId);
    }

    @Override
    public List<Statement> findByStatus(StatementStatus status) {
        return statementRepository.findByStatus(status)
                .stream().map(this::toDomain).toList();
    }

    private StatementEntity toEntity(Statement statement) {
        if (statement == null) return null;
        return StatementEntity.builder()
                .id(statement.getId())
                .customerId(statement.getCustomerId())
                .accountNumber(statement.getAccountNumber())
                .statementPeriod(statement.getStatementPeriod())
                .storagePath(statement.getStoragePath())
                .fileSizeBytes(statement.getFileSizeBytes())
                .openingBalance(statement.getOpeningBalance())
                .closingBalance(statement.getClosingBalance())
                .totalCredits(statement.getTotalCredits())
                .totalDebits(statement.getTotalDebits())
                .transactionCount(statement.getTransactionCount())
                .status(statement.getStatus())
                .generatedAt(statement.getGeneratedAt())
                .lastAccessedAt(statement.getLastAccessedAt())
                .createdAt(statement.getCreatedAt())
                .updatedAt(statement.getUpdatedAt())
                .build();
    }

    private Statement toDomain(StatementEntity entity) {
        if (entity == null) return null;
        return Statement.builder()
                .id(entity.getId())
                .customerId(entity.getCustomerId())
                .accountNumber(entity.getAccountNumber())
                .statementPeriod(entity.getStatementPeriod())
                .storagePath(entity.getStoragePath())
                .fileSizeBytes(entity.getFileSizeBytes())
                .openingBalance(entity.getOpeningBalance())
                .closingBalance(entity.getClosingBalance())
                .totalCredits(entity.getTotalCredits())
                .totalDebits(entity.getTotalDebits())
                .transactionCount(entity.getTransactionCount())
                .status(entity.getStatus())
                .generatedAt(entity.getGeneratedAt())
                .lastAccessedAt(entity.getLastAccessedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
