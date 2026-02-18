package id.payu.statement.integration;

import id.payu.statement.adapter.persistence.repository.StatementRepository;
import id.payu.statement.domain.entity.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Statement Repository.
 * Tests CRUD operations and custom queries.
 */
@DataJpaTest
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("Statement Repository Integration Tests")
class StatementRepositoryIntegrationTest {

    @Autowired
    private StatementRepository statementRepository;

    private static final String CUSTOMER_ID_1 = "CUST-001";
    private static final String CUSTOMER_ID_2 = "CUST-002";
    private static final String ACCOUNT_NUMBER = "1234567890";

    @BeforeEach
    void setUp() {
        statementRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save and retrieve statement")
    void saveAndFindById_shouldWork() {
        Statement statement = Statement.builder()
                .customerId(CUSTOMER_ID_1)
                .accountNumber(ACCOUNT_NUMBER)
                .statementPeriod(LocalDate.of(2026, 2, 1))
                .storagePath("s3://bucket/statement.pdf")
                .fileSizeBytes(1024L)
                .openingBalance(new BigDecimal("1000000.00"))
                .closingBalance(new BigDecimal("1200000.00"))
                .totalCredits(new BigDecimal("500000.00"))
                .totalDebits(new BigDecimal("300000.00"))
                .transactionCount(25)
                .status(Statement.StatementStatus.COMPLETED)
                .build();

        Statement saved = statementRepository.save(statement);
        Optional<Statement> found = statementRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCustomerId()).isEqualTo(CUSTOMER_ID_1);
        assertThat(found.get().getStatus()).isEqualTo(Statement.StatementStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should find statements by status")
    void findByStatus_shouldReturnStatementsWithStatus() {
        // Create statements for customer 1
        for (int i = 1; i <= 3; i++) {
            Statement statement = Statement.builder()
                    .customerId(CUSTOMER_ID_1)
                    .accountNumber(ACCOUNT_NUMBER)
                    .statementPeriod(LocalDate.of(2026, i, 1))
                    .storagePath("s3://bucket/statement-" + i + ".pdf")
                    .openingBalance(new BigDecimal("1000000.00"))
                    .closingBalance(new BigDecimal("1100000.00"))
                    .status(Statement.StatementStatus.COMPLETED)
                    .build();
            statementRepository.save(statement);
        }

        // Create statement for customer 2
        Statement statement2 = Statement.builder()
                .customerId(CUSTOMER_ID_2)
                .accountNumber("0987654321")
                .statementPeriod(LocalDate.of(2026, 1, 1))
                .storagePath("s3://bucket/statement-other.pdf")
                .openingBalance(new BigDecimal("2000000.00"))
                .closingBalance(new BigDecimal("2200000.00"))
                .status(Statement.StatementStatus.COMPLETED)
                .build();
        statementRepository.save(statement2);

        List<Statement> completedStatements = statementRepository.findByStatus(Statement.StatementStatus.COMPLETED);

        assertThat(completedStatements).hasSize(4);
        assertThat(completedStatements).allMatch(s -> s.getStatus().equals(Statement.StatementStatus.COMPLETED));
    }

    @Test
    @DisplayName("Should find statements with pagination and sorting")
    void findAllByCustomerIdWithPagination_shouldReturnPagedResults() {
        // Create 5 statements
        for (int i = 1; i <= 5; i++) {
            Statement statement = Statement.builder()
                    .customerId(CUSTOMER_ID_1)
                    .accountNumber(ACCOUNT_NUMBER)
                    .statementPeriod(LocalDate.of(2026, i, 1))
                    .storagePath("s3://bucket/statement-" + i + ".pdf")
                    .openingBalance(new BigDecimal("1000000.00"))
                    .closingBalance(new BigDecimal("1100000.00"))
                    .status(Statement.StatementStatus.COMPLETED)
                    .build();
            statementRepository.save(statement);
        }

        Pageable pageable = PageRequest.of(0, 2);
        Page<Statement> page = statementRepository.findAllByCustomerId(CUSTOMER_ID_1, pageable);

        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should find latest completed statement by customer ID")
    void findLatestCompletedByCustomerId_shouldReturnLatest() {
        // Create old statement
        Statement oldStatement = Statement.builder()
                .customerId(CUSTOMER_ID_1)
                .accountNumber(ACCOUNT_NUMBER)
                .statementPeriod(LocalDate.of(2026, 1, 1))
                .storagePath("s3://bucket/statement-jan.pdf")
                .openingBalance(new BigDecimal("1000000.00"))
                .closingBalance(new BigDecimal("1100000.00"))
                .status(Statement.StatementStatus.COMPLETED)
                .build();
        statementRepository.save(oldStatement);

        // Create latest statement
        Statement latestStatement = Statement.builder()
                .customerId(CUSTOMER_ID_1)
                .accountNumber(ACCOUNT_NUMBER)
                .statementPeriod(LocalDate.of(2026, 3, 1))
                .storagePath("s3://bucket/statement-mar.pdf")
                .openingBalance(new BigDecimal("1200000.00"))
                .closingBalance(new BigDecimal("1300000.00"))
                .status(Statement.StatementStatus.COMPLETED)
                .build();
        statementRepository.save(latestStatement);

        Optional<Statement> found = statementRepository
                .findLatestCompletedByCustomerId(CUSTOMER_ID_1);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(latestStatement.getId());
        assertThat(found.get().getStatementPeriod()).isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test
    @DisplayName("Should find statement by customer ID and statement period")
    void findByCustomerIdAndStatementPeriod_shouldReturnStatement() {
        LocalDate period = LocalDate.of(2026, 2, 1);

        Statement statement = Statement.builder()
                .customerId(CUSTOMER_ID_1)
                .accountNumber(ACCOUNT_NUMBER)
                .statementPeriod(period)
                .storagePath("s3://bucket/statement-feb.pdf")
                .openingBalance(new BigDecimal("1000000.00"))
                .closingBalance(new BigDecimal("1200000.00"))
                .status(Statement.StatementStatus.COMPLETED)
                .build();
        statementRepository.save(statement);

        Optional<Statement> found = statementRepository
                .findByCustomerIdAndStatementPeriod(CUSTOMER_ID_1, period);

        assertThat(found).isPresent();
        assertThat(found.get().getStatementPeriod()).isEqualTo(period);
    }

    @Test
    @DisplayName("Should find statements by status")
    void findByStatus_shouldReturnStatementsWithStatus() {
        // Create completed statement
        Statement completedStatement = Statement.builder()
                .customerId(CUSTOMER_ID_1)
                .accountNumber(ACCOUNT_NUMBER)
                .statementPeriod(LocalDate.of(2026, 1, 1))
                .storagePath("s3://bucket/statement-1.pdf")
                .openingBalance(new BigDecimal("1000000.00"))
                .closingBalance(new BigDecimal("1100000.00"))
                .status(Statement.StatementStatus.COMPLETED)
                .build();
        statementRepository.save(completedStatement);

        // Create generating statement
        Statement generatingStatement = Statement.builder()
                .customerId(CUSTOMER_ID_1)
                .accountNumber(ACCOUNT_NUMBER)
                .statementPeriod(LocalDate.of(2026, 2, 1))
                .storagePath("s3://bucket/statement-2.pdf")
                .openingBalance(new BigDecimal("1100000.00"))
                .closingBalance(new BigDecimal("1200000.00"))
                .status(Statement.StatementStatus.GENERATING)
                .build();
        statementRepository.save(generatingStatement);

        List<Statement> completedStatements = statementRepository
                .findByStatus(Statement.StatementStatus.COMPLETED);

        assertThat(completedStatements).hasSize(1);
        assertThat(completedStatements.get(0).getStatus()).isEqualTo(Statement.StatementStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should count statements by customer ID")
    void countByCustomerId_shouldReturnCount() {
        // Create 3 statements
        for (int i = 1; i <= 3; i++) {
            Statement statement = Statement.builder()
                    .customerId(CUSTOMER_ID_1)
                    .accountNumber(ACCOUNT_NUMBER)
                    .statementPeriod(LocalDate.of(2026, i, 1))
                    .storagePath("s3://bucket/statement-" + i + ".pdf")
                    .openingBalance(new BigDecimal("1000000.00"))
                    .closingBalance(new BigDecimal("1100000.00"))
                    .status(Statement.StatementStatus.COMPLETED)
                    .build();
            statementRepository.save(statement);
        }

        long count = statementRepository.countByCustomerId(CUSTOMER_ID_1);

        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Should update statement status")
    void updateStatus_shouldWork() {
        Statement statement = Statement.builder()
                .customerId(CUSTOMER_ID_1)
                .accountNumber(ACCOUNT_NUMBER)
                .statementPeriod(LocalDate.of(2026, 2, 1))
                .storagePath("s3://bucket/statement.pdf")
                .openingBalance(new BigDecimal("1000000.00"))
                .closingBalance(new BigDecimal("1200000.00"))
                .status(Statement.StatementStatus.GENERATING)
                .build();
        statement = statementRepository.save(statement);

        // Update status
        statement.setStatus(Statement.StatementStatus.COMPLETED);
        statementRepository.save(statement);

        Optional<Statement> updated = statementRepository.findById(statement.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getStatus()).isEqualTo(Statement.StatementStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should delete statement")
    void delete_shouldWork() {
        Statement statement = Statement.builder()
                .customerId(CUSTOMER_ID_1)
                .accountNumber(ACCOUNT_NUMBER)
                .statementPeriod(LocalDate.of(2026, 2, 1))
                .storagePath("s3://bucket/statement.pdf")
                .openingBalance(new BigDecimal("1000000.00"))
                .closingBalance(new BigDecimal("1200000.00"))
                .status(Statement.StatementStatus.COMPLETED)
                .build();
        statement = statementRepository.save(statement);

        UUID id = statement.getId();
        statementRepository.deleteById(id);

        Optional<Statement> found = statementRepository.findById(id);
        assertThat(found).isEmpty();
    }
}
