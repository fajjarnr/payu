package id.payu.transaction.application.service;

import id.payu.transaction.domain.model.Disbursement;
import id.payu.transaction.domain.model.DisbursementStatus;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.port.out.BifastServicePort;
import id.payu.transaction.domain.port.out.DisbursementRepositoryPort;
import id.payu.transaction.domain.port.out.WalletServicePort;
import id.payu.transaction.dto.BifastTransferResponse;
import id.payu.transaction.dto.ReserveBalanceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DisbursementService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DisbursementService Tests")
class DisbursementServiceTest {

    @Mock
    private DisbursementRepositoryPort disbursementRepository;

    @Mock
    private WalletServicePort walletService;

    @Mock
    private BifastServicePort bifastService;

    @InjectMocks
    private DisbursementService disbursementService;

    private static final UUID SOURCE_ACCOUNT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID DISBURSEMENT_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
    private static final String BANK_CODE = "014";
    private static final String ACCOUNT_NUMBER = "1234567890";
    private static final String ACCOUNT_NAME = "John Doe";
    private static final Money AMOUNT = Money.idr("100000");

    @Nested
    @DisplayName("Create Disbursement")
    class CreateDisbursementTests {

        @Test
        @DisplayName("Should create disbursement with idempotency check")
        void shouldCreateDisbursementWithIdempotencyCheck() {
            // Given
            String idempotencyKey = "idem-key-123";
            when(disbursementRepository.findByIdempotencyKey(idempotencyKey))
                    .thenReturn(Optional.empty());
            when(walletService.reserveBalance(any(), any(), any()))
                    .thenReturn(new ReserveBalanceResponse("res-123", true, "Reserved"));
            when(disbursementRepository.save(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Disbursement result = disbursementService.createDisbursement(
                    SOURCE_ACCOUNT_ID, AMOUNT, BANK_CODE, ACCOUNT_NUMBER, ACCOUNT_NAME,
                    "Test description", idempotencyKey
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(DisbursementStatus.PENDING);
            assertThat(result.getIdempotencyKey()).isEqualTo(idempotencyKey);
            verify(walletService).reserveBalance(eq(SOURCE_ACCOUNT_ID), any(), eq(AMOUNT.getAmount()));
            verify(disbursementRepository).save(any());
        }

        @Test
        @DisplayName("Should return existing disbursement for duplicate idempotency key")
        void shouldReturnExistingDisbursementForDuplicateIdempotencyKey() {
            // Given
            String idempotencyKey = "idem-key-123";
            Disbursement existing = Disbursement.createWithIdempotencyKey(
                    SOURCE_ACCOUNT_ID, AMOUNT, BANK_CODE, ACCOUNT_NUMBER, ACCOUNT_NAME, idempotencyKey
            );
            when(disbursementRepository.findByIdempotencyKey(idempotencyKey))
                    .thenReturn(Optional.of(existing));

            // When
            Disbursement result = disbursementService.createDisbursement(
                    SOURCE_ACCOUNT_ID, AMOUNT, BANK_CODE, ACCOUNT_NUMBER, ACCOUNT_NAME,
                    "Test description", idempotencyKey
            );

            // Then
            assertThat(result).isEqualTo(existing);
            verify(walletService, never()).reserveBalance(any(), any(), any());
            verify(disbursementRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Process Disbursement")
    class ProcessDisbursementTests {

        @Test
        @DisplayName("Should process pending disbursement")
        void shouldProcessPendingDisbursement() {
            // Given
            Disbursement disbursement = Disbursement.createWithIdempotencyKey(
                    SOURCE_ACCOUNT_ID, AMOUNT, BANK_CODE, ACCOUNT_NUMBER, ACCOUNT_NAME, "idem-123"
            );
            when(disbursementRepository.findById(any()))
                    .thenReturn(Optional.of(disbursement));
            when(bifastService.initiateTransfer(any()))
                    .thenReturn(BifastTransferResponse.builder()
                            .success(true)
                            .referenceNumber("BIFAST-123")
                            .build());
            when(disbursementRepository.save(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Disbursement result = disbursementService.processDisbursement(disbursement.getId());

            // Then
            assertThat(result.getStatus()).isEqualTo(DisbursementStatus.PROCESSING);
            assertThat(result.getProcessedAt()).isNotNull();
            verify(bifastService).initiateTransfer(any());
        }
    }

    @Nested
    @DisplayName("Complete Disbursement")
    class CompleteDisbursementTests {

        @Test
        @DisplayName("Should complete processing disbursement")
        void shouldCompleteProcessingDisbursement() {
            // Given
            Disbursement disbursement = Disbursement.createWithIdempotencyKey(
                    SOURCE_ACCOUNT_ID, AMOUNT, BANK_CODE, ACCOUNT_NUMBER, ACCOUNT_NAME, "idem-123"
            );
            disbursement.process();

            when(disbursementRepository.findById(any()))
                    .thenReturn(Optional.of(disbursement));
            when(disbursementRepository.save(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Disbursement result = disbursementService.completeDisbursement(disbursement.getId(), "BANK-REF-123");

            // Then
            assertThat(result.getStatus()).isEqualTo(DisbursementStatus.COMPLETED);
            assertThat(result.getBankReference()).isEqualTo("BANK-REF-123");
            assertThat(result.getCompletedAt()).isNotNull();
            verify(walletService).commitBalance(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Fail Disbursement")
    class FailDisbursementTests {

        @Test
        @DisplayName("Should fail processing disbursement and release funds")
        void shouldFailProcessingDisbursementAndReleaseFunds() {
            // Given
            Disbursement disbursement = Disbursement.createWithIdempotencyKey(
                    SOURCE_ACCOUNT_ID, AMOUNT, BANK_CODE, ACCOUNT_NUMBER, ACCOUNT_NAME, "idem-123"
            );
            disbursement.process();

            when(disbursementRepository.findById(any()))
                    .thenReturn(Optional.of(disbursement));
            when(disbursementRepository.save(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Disbursement result = disbursementService.failDisbursement(disbursement.getId(), "Invalid account");

            // Then
            assertThat(result.getStatus()).isEqualTo(DisbursementStatus.FAILED);
            assertThat(result.getFailureReason()).isEqualTo("Invalid account");
            assertThat(result.getCompletedAt()).isNotNull();
            verify(walletService).releaseBalance(any(), any(), any(), any());
        }
    }
}
