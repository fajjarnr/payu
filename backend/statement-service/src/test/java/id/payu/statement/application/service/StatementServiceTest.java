package id.payu.statement.application.service;

import id.payu.statement.adapter.client.TransactionServiceClient;
import id.payu.statement.dto.TransactionRecord;
import id.payu.statement.dto.TransactionType;
import id.payu.statement.adapter.client.WalletServiceClient;
import id.payu.statement.adapter.persistence.entity.StatementEntity;
import id.payu.statement.adapter.persistence.repository.StatementRepository;
import id.payu.statement.application.service.dto.StatementGenerationRequest;
import id.payu.statement.application.service.dto.StatementResponse;
import id.payu.statement.application.service.exception.StatementException;
import id.payu.statement.domain.entity.StatementStatus;
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
import id.payu.outbox.service.OutboxService;

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
    private OutboxService outboxService;

    @Mock
    private WalletServiceClient walletServiceClient;

    @Mock
    private TransactionServiceClient transactionServiceClient;

    @Mock
    private id.payu.statement.adapter.storage.S3StorageAdapter s3StorageAdapter;

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
                .status(StatementStatus.COMPLETED)
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
            assertThat(result.getStatus()).isEqualTo(StatementStatus.COMPLETED);
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
    @DisplayName("generateStatement (ledger balance_after — IMP-3)")
    class GenerateStatementLedgerBalances {

        private StatementGenerationRequest request;

        @BeforeEach
        void init() {
            org.springframework.test.util.ReflectionTestUtils.setField(statementService, "companyName", "PayU");
            org.springframework.test.util.ReflectionTestUtils.setField(statementService, "companyAddress", "Jakarta");
            org.springframework.test.util.ReflectionTestUtils.setField(statementService, "storagePath", "/tmp/statements");
            request = new StatementGenerationRequest(testUserId.toString(), testAccountNumber, 2024, 1);
            when(statementRepository.existsByCustomerIdAndStatementPeriod(testUserId.toString(), LocalDate.of(2024, 1, 1)))
                    .thenReturn(false);
            when(statementRepository.save(any(StatementEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(s3StorageAdapter.isEnabled()).thenReturn(true);
            when(s3StorageAdapter.uploadPdf(any(), any())).thenReturn("/s3/statement.pdf");
        }

        @Test
        @DisplayName("should use ledger balance_after snapshot for closing and opening balance")
        void shouldUseLedgerBalanceAfterForPastPeriodBalances() {
            LocalDate period = LocalDate.of(2024, 1, 1);
            LocalDate endDate = period.plusMonths(1).minusDays(1);
            when(walletServiceClient.getCurrentBalance(testUserId.toString()))
                    .thenReturn(new BigDecimal("25000000"));
            when(transactionServiceClient.getTransactions(testUserId.toString(), period, endDate))
                    .thenReturn(List.of());
            when(walletServiceClient.getBalanceAsOf(testUserId.toString(), endDate))
                    .thenReturn(Optional.of(new BigDecimal("15000000")));
            when(walletServiceClient.getBalanceAsOf(testUserId.toString(), period.minusDays(1)))
                    .thenReturn(Optional.of(new BigDecimal("10000000")));

            statementService.generateStatement(request);

            ArgumentCaptor<StatementEntity> captor = ArgumentCaptor.forClass(StatementEntity.class);
            verify(statementRepository, atLeast(2)).save(captor.capture());
            StatementEntity saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
            assertThat(saved.getClosingBalance()).isEqualByComparingTo("15000000");
            assertThat(saved.getOpeningBalance()).isEqualByComparingTo("10000000");
            verify(transactionServiceClient, never()).getTransactions(
                    eq(testUserId.toString()), eq(endDate.plusDays(1)), any());
        }

        @Test
        @DisplayName("should fall back to derivation when no ledger snapshot exists")
        void shouldFallBackToDerivationWithoutLedgerSnapshot() {
            LocalDate period = LocalDate.of(2024, 1, 1);
            LocalDate endDate = period.plusMonths(1).minusDays(1);
            when(walletServiceClient.getCurrentBalance(testUserId.toString()))
                    .thenReturn(new BigDecimal("15000000"));
            when(transactionServiceClient.getTransactions(testUserId.toString(), period, endDate))
                    .thenReturn(List.of(new TransactionRecord(
                            LocalDate.of(2024, 1, 15), "topup", new BigDecimal("2000000"),
                            TransactionType.CREDIT)));
            when(transactionServiceClient.getTransactions(testUserId.toString(), endDate.plusDays(1), LocalDate.now()))
                    .thenReturn(List.of(new TransactionRecord(
                            LocalDate.of(2024, 2, 5), "purchase", new BigDecimal("5000000"),
                            TransactionType.DEBIT)));
            when(walletServiceClient.getBalanceAsOf(anyString(), any()))
                    .thenReturn(Optional.empty());

            statementService.generateStatement(request);

            ArgumentCaptor<StatementEntity> captor = ArgumentCaptor.forClass(StatementEntity.class);
            verify(statementRepository, atLeast(2)).save(captor.capture());
            StatementEntity saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
            assertThat(saved.getClosingBalance()).isEqualByComparingTo("20000000");
            assertThat(saved.getOpeningBalance()).isEqualByComparingTo("18000000");
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
            testStatement.setStatus(StatementStatus.GENERATING);
            when(statementRepository.findByIdAndCustomerId(testStatementId, testUserId.toString()))
                    .thenReturn(Optional.of(testStatement));

            assertThatThrownBy(() -> statementService.getStatementPdf(testStatementId, testUserId.toString()))
                    .isInstanceOf(StatementException.class)
                    .hasMessageContaining("StatementEntity is not ready for download");
        }

        @Test
        @DisplayName("BUG-STMT-PATH-001: should throw StatementException when storagePath is null")
        void shouldThrowExceptionWhenStoragePathIsNull() {
            testStatement.setStatus(StatementStatus.COMPLETED);
            testStatement.setStoragePath(null);
            when(statementRepository.findByIdAndCustomerId(testStatementId, testUserId.toString()))
                    .thenReturn(Optional.of(testStatement));

            assertThatThrownBy(() -> statementService.getStatementPdf(testStatementId, testUserId.toString()))
                    .isInstanceOf(StatementException.class)
                    .hasMessageContaining("storage path");
        }

        @Test
        @DisplayName("STMT-S3-001: should download PDF from S3 adapter when storage path is an s3:// URI")
        void shouldDownloadFromS3WhenStoragePathIsS3Uri() throws Exception {
            testStatement.setStatus(StatementStatus.COMPLETED);
            testStatement.setStoragePath("s3://payu-statements/statements/statement_" + testStatementId + ".pdf");
            when(statementRepository.findByIdAndCustomerId(testStatementId, testUserId.toString()))
                    .thenReturn(Optional.of(testStatement));
            when(s3StorageAdapter.isEnabled()).thenReturn(true);
            byte[] pdfBytes = "PDF-S3-CONTENT".getBytes();
            when(s3StorageAdapter.downloadPdf(anyString())).thenReturn(pdfBytes);

            byte[] result = statementService.getStatementPdf(testStatementId, testUserId.toString());

            assertThat(result).isEqualTo(pdfBytes);
            verify(s3StorageAdapter).downloadPdf("s3://payu-statements/statements/statement_" + testStatementId + ".pdf");
            verify(s3StorageAdapter, never()).uploadPdf(any(), any());
        }

        @Test
        @DisplayName("STMT-S3-001: should fall back to local file read when S3 is disabled")
        void shouldReadLocalFileWhenS3Disabled() throws Exception {
            testStatement.setStatus(StatementStatus.COMPLETED);
            java.nio.file.Path localPath = java.nio.file.Files.createTempFile("stmt-s3-test", ".pdf");
            java.nio.file.Files.write(localPath, "PDF-LOCAL-CONTENT".getBytes());
            testStatement.setStoragePath(localPath.toString());
            when(statementRepository.findByIdAndCustomerId(testStatementId, testUserId.toString()))
                    .thenReturn(Optional.of(testStatement));
            when(s3StorageAdapter.isEnabled()).thenReturn(false);

            byte[] result = statementService.getStatementPdf(testStatementId, testUserId.toString());

            assertThat(new String(result)).isEqualTo("PDF-LOCAL-CONTENT");
            verify(s3StorageAdapter, never()).downloadPdf(anyString());
            localPath.toFile().deleteOnExit();
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
            assertThat(savedStatements).anyMatch(s -> s.getStatus() == StatementStatus.GENERATING);
        }
    }
}
