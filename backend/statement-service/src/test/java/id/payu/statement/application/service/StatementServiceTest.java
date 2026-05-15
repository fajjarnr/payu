package id.payu.statement.application.service;

import id.payu.statement.adapter.persistence.entity.StatementEntity;
import id.payu.statement.adapter.persistence.repository.StatementRepository;
import id.payu.statement.application.service.dto.StatementGenerationRequest;
import id.payu.statement.application.service.dto.StatementResponse;
import id.payu.statement.application.service.exception.StatementException;
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
    private StatementEntity testStatement;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testAccountNumber = "1234567890";
        testStatementId = UUID.randomUUID();

        testStatement = StatementEntity.builder()
                .id(testStatementId)
                .customerId(testUserId.toString())
                .accountNumber(testAccountNumber)
                .statementPeriod(LocalDate.of(2024, 1, 1))
                .status(StatementEntity.StatementStatus.COMPLETED)
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
            when(statementRepository.findByIdAndCustomerId(testStatementId, testUserId.toString()))
                    .thenReturn(Optional.of(testStatement));

            StatementResponse result = statementService.getStatement(testStatementId, testUserId.toString());

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testStatementId);
            assertThat(result.getCustomerId()).isEqualTo(testUserId.toString());
            assertThat(result.getStatus()).isEqualTo(StatementEntity.StatementStatus.COMPLETED);
            verify(statementRepository).save(any(StatementEntity.class));
        }

        @Test
        @DisplayName("should throw exception when statement not found")
        void shouldThrowExceptionWhenStatementNotFound() {
            when(statementRepository.findByIdAndCustomerId(testStatementId, testUserId.toString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> statementService.getStatement(testStatementId, testUserId.toString()))
                    .isInstanceOf(StatementException.class)
                    .hasMessageContaining("StatementEntity not found");
        }

        @Test
        @DisplayName("should record access when getting statement")
        void shouldRecordAccessWhenGettingStatement() {
            when(statementRepository.findByIdAndCustomerId(testStatementId, testUserId.toString()))
                    .thenReturn(Optional.of(testStatement));

            statementService.getStatement(testStatementId, testUserId.toString());

            ArgumentCaptor<StatementEntity> captor = ArgumentCaptor.forClass(StatementEntity.class);
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
            Page<StatementEntity> statementPage = new PageImpl<>(List.of(testStatement), pageable, 1);

            when(statementRepository.findAllByCustomerId(testUserId.toString(), pageable))
                    .thenReturn(statementPage);

            Page<StatementResponse> result = statementService.listStatements(testUserId.toString(), pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(testStatementId);
        }

        @Test
        @DisplayName("should return empty page when no statements found")
        void shouldReturnEmptyPageWhenNoStatementsFound() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<StatementEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(statementRepository.findAllByCustomerId(testUserId.toString(), pageable))
                    .thenReturn(emptyPage);

            Page<StatementResponse> result = statementService.listStatements(testUserId.toString(), pageable);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getLatestStatement")
    class GetLatestStatement {

        @Test
        @DisplayName("should get latest completed statement")
        void shouldGetLatestCompletedStatement() {
            when(statementRepository.findLatestCompletedByCustomerId(testUserId.toString()))
                    .thenReturn(Optional.of(testStatement));

            Optional<StatementResponse> result = statementService.getLatestStatement(testUserId.toString());

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(testStatementId);
        }

        @Test
        @DisplayName("should return empty when no statement found")
        void shouldReturnEmptyWhenNoStatementFound() {
            when(statementRepository.findLatestCompletedByCustomerId(testUserId.toString()))
                    .thenReturn(Optional.empty());

            Optional<StatementResponse> result = statementService.getLatestStatement(testUserId.toString());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getStatementPdf")
    class GetStatementPdf {

        @Test
        @DisplayName("should throw exception when statement not found")
        void shouldThrowExceptionWhenStatementNotFound() {
            when(statementRepository.findByIdAndCustomerId(testStatementId, testUserId.toString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> statementService.getStatementPdf(testStatementId, testUserId.toString()))
                    .isInstanceOf(StatementException.class)
                    .hasMessageContaining("StatementEntity not found");
        }

        @Test
        @DisplayName("should throw exception when statement not completed")
        void shouldThrowExceptionWhenStatementNotCompleted() {
            testStatement.setStatus(StatementEntity.StatementStatus.GENERATING);
            when(statementRepository.findByIdAndCustomerId(testStatementId, testUserId.toString()))
                    .thenReturn(Optional.of(testStatement));

            assertThatThrownBy(() -> statementService.getStatementPdf(testStatementId, testUserId.toString()))
                    .isInstanceOf(StatementException.class)
                    .hasMessageContaining("StatementEntity is not ready for download");
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
                    .hasMessageContaining("StatementEntity not found");
        }

        @Test
        @DisplayName("should reset status to generating")
        void shouldResetStatusToGenerating() {
            when(statementRepository.findById(testStatementId))
                    .thenReturn(Optional.of(testStatement));
            when(statementRepository.save(any(StatementEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            // Mock the exists check to return true so generateStatement doesn't run
            when(statementRepository.existsByCustomerIdAndStatementPeriod(any(), any()))
                    .thenReturn(true);

            statementService.regenerateStatement(testStatementId);

            ArgumentCaptor<StatementEntity> captor = ArgumentCaptor.forClass(StatementEntity.class);
            verify(statementRepository, atLeast(1)).save(captor.capture());

            // One of the saved statements should have GENERATING status
            List<StatementEntity> savedStatements = captor.getAllValues();
            assertThat(savedStatements).anyMatch(s -> s.getStatus() == StatementEntity.StatementStatus.GENERATING);
        }
    }
}
