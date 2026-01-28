package id.payu.statement.service;

import id.payu.statement.domain.entity.Statement;
import id.payu.statement.domain.repository.StatementRepository;
import id.payu.statement.service.dto.StatementGenerationRequest;
import id.payu.statement.service.dto.StatementResponse;
import id.payu.statement.service.exception.StatementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatementService")
class StatementServiceTest {

    @Mock
    private StatementRepository statementRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private WalletServiceClient walletServiceClient;

    @Mock
    private TransactionServiceClient transactionServiceClient;

    @InjectMocks
    private StatementService statementService;

    private UUID testUserId;
    private String testAccountNumber;
    private UUID testStatementId;
    private Statement testStatement;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testAccountNumber = "1234567890";
        testStatementId = UUID.randomUUID();

        testStatement = Statement.builder()
                .id(testStatementId)
                .userId(testUserId)
                .accountNumber(testAccountNumber)
                .statementPeriod(LocalDate.of(2024, 1, 1))
                .status(Statement.StatementStatus.COMPLETED)
                .openingBalance(new BigDecimal("10000000"))
                .closingBalance(new BigDecimal("15000000"))
                .totalCredits(new BigDecimal("10000000"))
                .totalDebits(new BigDecimal("5000000"))
                .transactionCount(15)
                .storagePath("/tmp/statements/statement_" + testStatementId + ".pdf")
                .fileSizeBytes(1024L)
                .generatedAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getStatement")
    class GetStatement {

        @Test
        @DisplayName("should get statement successfully")
        void shouldGetStatementSuccessfully() {
            when(statementRepository.findByIdAndUserId(testStatementId, testUserId))
                    .thenReturn(Optional.of(testStatement));

            StatementResponse result = statementService.getStatement(testStatementId, testUserId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testStatementId);
            assertThat(result.getUserId()).isEqualTo(testUserId);
            assertThat(result.getStatus()).isEqualTo(Statement.StatementStatus.COMPLETED);
            verify(statementRepository).save(any(Statement.class));
        }

        @Test
        @DisplayName("should throw exception when statement not found")
        void shouldThrowExceptionWhenStatementNotFound() {
            when(statementRepository.findByIdAndUserId(testStatementId, testUserId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> statementService.getStatement(testStatementId, testUserId))
                    .isInstanceOf(StatementException.class)
                    .hasMessageContaining("Statement not found");
        }

        @Test
        @DisplayName("should record access when getting statement")
        void shouldRecordAccessWhenGettingStatement() {
            when(statementRepository.findByIdAndUserId(testStatementId, testUserId))
                    .thenReturn(Optional.of(testStatement));

            statementService.getStatement(testStatementId, testUserId);

            ArgumentCaptor<Statement> captor = ArgumentCaptor.forClass(Statement.class);
            verify(statementRepository).save(captor.capture());
            assertThat(captor.getValue().getLastAccessedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("listStatements")
    class ListStatements {

        @Test
        @DisplayName("should list statements for user")
        void shouldListStatementsForUser() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Statement> statementPage = new PageImpl<>(List.of(testStatement), pageable, 1);

            when(statementRepository.findAllByUserId(testUserId, pageable))
                    .thenReturn(statementPage);

            Page<StatementResponse> result = statementService.listStatements(testUserId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(testStatementId);
        }

        @Test
        @DisplayName("should return empty page when no statements found")
        void shouldReturnEmptyPageWhenNoStatementsFound() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Statement> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(statementRepository.findAllByUserId(testUserId, pageable))
                    .thenReturn(emptyPage);

            Page<StatementResponse> result = statementService.listStatements(testUserId, pageable);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getLatestStatement")
    class GetLatestStatement {

        @Test
        @DisplayName("should get latest completed statement")
        void shouldGetLatestCompletedStatement() {
            when(statementRepository.findLatestCompletedByUserId(testUserId))
                    .thenReturn(Optional.of(testStatement));

            Optional<StatementResponse> result = statementService.getLatestStatement(testUserId);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(testStatementId);
        }

        @Test
        @DisplayName("should return empty when no statement found")
        void shouldReturnEmptyWhenNoStatementFound() {
            when(statementRepository.findLatestCompletedByUserId(testUserId))
                    .thenReturn(Optional.empty());

            Optional<StatementResponse> result = statementService.getLatestStatement(testUserId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getStatementPdf")
    class GetStatementPdf {

        @Test
        @DisplayName("should throw exception when statement not found")
        void shouldThrowExceptionWhenStatementNotFound() {
            when(statementRepository.findByIdAndUserId(testStatementId, testUserId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> statementService.getStatementPdf(testStatementId, testUserId))
                    .isInstanceOf(StatementException.class)
                    .hasMessageContaining("Statement not found");
        }

        @Test
        @DisplayName("should throw exception when statement not completed")
        void shouldThrowExceptionWhenStatementNotCompleted() {
            testStatement.setStatus(Statement.StatementStatus.GENERATING);
            when(statementRepository.findByIdAndUserId(testStatementId, testUserId))
                    .thenReturn(Optional.of(testStatement));

            assertThatThrownBy(() -> statementService.getStatementPdf(testStatementId, testUserId))
                    .isInstanceOf(StatementException.class)
                    .hasMessageContaining("Statement is not ready for download");
        }
    }

    @Nested
    @DisplayName("regenerateStatement")
    class RegenerateStatement {

        @Test
        @DisplayName("should throw exception when statement not found")
        void shouldThrowExceptionWhenStatementNotFound() {
            when(statementRepository.findById(testStatementId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> statementService.regenerateStatement(testStatementId))
                    .isInstanceOf(StatementException.class)
                    .hasMessageContaining("Statement not found");
        }

        @Test
        @DisplayName("should reset status to generating")
        void shouldResetStatusToGenerating() {
            when(statementRepository.findById(testStatementId))
                    .thenReturn(Optional.of(testStatement));
            when(statementRepository.save(any(Statement.class))).thenAnswer(inv -> inv.getArgument(0));
            // Mock the exists check to return true so generateStatement doesn't run
            when(statementRepository.existsByUserIdAndStatementPeriod(any(), any()))
                    .thenReturn(true);

            statementService.regenerateStatement(testStatementId);

            ArgumentCaptor<Statement> captor = ArgumentCaptor.forClass(Statement.class);
            verify(statementRepository, atLeast(1)).save(captor.capture());

            // One of the saved statements should have GENERATING status
            List<Statement> savedStatements = captor.getAllValues();
            assertThat(savedStatements).anyMatch(s -> s.getStatus() == Statement.StatementStatus.GENERATING);
        }
    }
}
