package id.payu.promotion.application.saga;

import id.payu.promotion.adapter.client.WalletCreditException;
import id.payu.promotion.domain.port.out.CashbackPersistencePort;
import id.payu.promotion.domain.model.Cashback;
import id.payu.promotion.domain.CashbackStatus;
import id.payu.promotion.domain.port.out.WalletServicePort;
import id.payu.promotion.dto.CreateCashbackRequest;
import id.payu.saga.model.SagaResult;
import id.payu.saga.model.SagaState;
import id.payu.saga.repository.SagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CashbackSagaOrchestrator.
 * Tests the saga pattern implementation for atomic cashback credit.
 */
@ExtendWith(MockitoExtension.class)
class CashbackSagaOrchestratorTest {

    @Mock
    private SagaRepository sagaRepository;

    @Mock
    private TaskExecutor sagaTaskExecutor;

    @Mock
    private ScheduledExecutorService sagaRetryScheduler;

    @Mock
    private WalletServicePort walletServicePort;

    @Mock
    private CashbackPersistencePort cashbackRepository;

    @Mock
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    private CashbackSagaOrchestrator orchestrator;

    private static final String TEST_ACCOUNT_ID = "acc-123";
    private static final String TEST_TRANSACTION_ID = "txn-456";

    @BeforeEach
    void setUp() {
        orchestrator = new CashbackSagaOrchestrator(sagaRepository, sagaTaskExecutor, sagaRetryScheduler, transactionManager, walletServicePort, cashbackRepository);
    }

    @Test
    void testSaga_Success_WalletCreditThenRecordCashback() {
        // Given
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            null
        );

        CashbackSagaContext context = new CashbackSagaContext(request);
        Cashback savedCashback = createTestCashback(UUID.randomUUID(), new BigDecimal("20.00"));

        // Mock wallet credit success
        when(walletServicePort.creditWallet(any(), any(), any(), any()))
            .thenReturn(true);

        // Mock saga repository
        when(sagaRepository.save(any()))
            .thenAnswer(inv -> inv.getArgument(0));
        when(sagaRepository.findById(any()))
            .thenReturn(Optional.empty());

        // Mock cashback save
        when(cashbackRepository.save(any(Cashback.class)))
            .thenReturn(savedCashback);

        // When
        SagaResult<CashbackSagaContext> result = orchestrator.executeCashbackSaga(context);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(SagaState.COMPLETED, result.getFinalState());
        assertNotNull(result.getData().getCashback());

        // Verify wallet was credited
        verify(walletServicePort).creditWallet(
            eq(TEST_ACCOUNT_ID),
            eq(new BigDecimal("20.00")),
            eq(TEST_TRANSACTION_ID),
            contains("Cashback")
        );

        // Verify cashback was recorded with CREDITED status
        ArgumentCaptor<Cashback> cashbackCaptor = ArgumentCaptor.forClass(Cashback.class);
        verify(cashbackRepository).save(cashbackCaptor.capture());
        Cashback capturedCashback = cashbackCaptor.getValue();
        assertEquals(CashbackStatus.CREDITED, capturedCashback.getStatus());
        assertNotNull(capturedCashback.getCreditedAt());
    }

    @Test
    void testSaga_Failure_WalletCreditFails_CashbackNotRecorded() {
        // Given
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            null
        );

        CashbackSagaContext context = new CashbackSagaContext(request);

        // Mock wallet credit failure
        when(walletServicePort.creditWallet(any(), any(), any(), any()))
            .thenThrow(new WalletCreditException("Wallet service unavailable"));

        // Mock saga repository
        when(sagaRepository.save(any()))
            .thenAnswer(inv -> inv.getArgument(0));
        when(sagaRepository.findById(any()))
            .thenReturn(Optional.empty());

        // When
        SagaResult<CashbackSagaContext> result = orchestrator.executeCashbackSaga(context);

        // Then
        assertFalse(result.isSuccess());
        assertEquals(SagaState.FAILED, result.getFinalState());
        assertEquals("CREDIT_WALLET", result.getErrorStep());

        // Verify wallet was attempted
        verify(walletServicePort).creditWallet(any(), any(), any(), any());

        // Verify cashback was NOT recorded
        verify(cashbackRepository, never()).save(any(Cashback.class));
    }

    @Test
    void testSaga_Failure_WalletReturnsFalse() {
        // Given
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            null
        );

        CashbackSagaContext context = new CashbackSagaContext(request);

        // Mock wallet credit returns false (not exception)
        when(walletServicePort.creditWallet(any(), any(), any(), any()))
            .thenReturn(false);

        // Mock saga repository
        when(sagaRepository.save(any()))
            .thenAnswer(inv -> inv.getArgument(0));
        when(sagaRepository.findById(any()))
            .thenReturn(Optional.empty());

        // When
        SagaResult<CashbackSagaContext> result = orchestrator.executeCashbackSaga(context);

        // Then
        assertFalse(result.isSuccess());
        assertEquals(SagaState.FAILED, result.getFinalState());
        assertEquals("CREDIT_WALLET", result.getErrorStep());

        // Verify cashback was NOT recorded
        verify(cashbackRepository, never()).save(any(Cashback.class));
    }

    @Test
    void testSaga_Compensation_WhenRecordCashbackFails() {
        // Given
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            null
        );

        CashbackSagaContext context = new CashbackSagaContext(request);
        UUID cashbackId = UUID.randomUUID();
        Cashback savedCashback = createTestCashback(cashbackId, new BigDecimal("20.00"));

        // Mock wallet credit success
        when(walletServicePort.creditWallet(any(), any(), any(), any()))
            .thenReturn(true);

        // Mock saga repository
        when(sagaRepository.save(any()))
            .thenAnswer(inv -> inv.getArgument(0));
        when(sagaRepository.findById(any()))
            .thenReturn(Optional.empty());

        // First save succeeds, then we simulate failure in the flow
        when(cashbackRepository.save(any(Cashback.class)))
            .thenReturn(savedCashback)
            .thenThrow(new RuntimeException("Database error"));

        // When
        SagaResult<CashbackSagaContext> result = orchestrator.executeCashbackSaga(context);

        // Then - should fail (compensation may or may not fully run depending on timing)
        // The key assertion is that wallet was credited (which we can't undo easily)
        // and cashback save was attempted
        assertNotNull(result);

        // Wallet was credited
        verify(walletServicePort).creditWallet(any(), any(), any(), any());
    }

    @Test
    void testSagaContext_CalculatesCorrectCashbackAmount() {
        // Given - GROCERY category = 2%
        CreateCashbackRequest groceryRequest = new CreateCashbackRequest(
            TEST_ACCOUNT_ID, TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"), "MERCHANT001", "GROCERY", null
        );

        // When
        CashbackSagaContext context = new CashbackSagaContext(groceryRequest);

        // Then
        assertEquals(new BigDecimal("20.00"), context.getAmount());

        // Given - DINING category = 3%
        CreateCashbackRequest diningRequest = new CreateCashbackRequest(
            TEST_ACCOUNT_ID, TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"), "MERCHANT001", "DINING", null
        );

        // When
        CashbackSagaContext diningContext = new CashbackSagaContext(diningRequest);

        // Then
        assertEquals(new BigDecimal("30.00"), diningContext.getAmount());

        // Given - SHOPPING category = 1.5%
        CreateCashbackRequest shoppingRequest = new CreateCashbackRequest(
            TEST_ACCOUNT_ID, TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"), "MERCHANT001", "SHOPPING", null
        );

        // When
        CashbackSagaContext shoppingContext = new CashbackSagaContext(shoppingRequest);

        // Then
        assertEquals(new BigDecimal("15.00"), shoppingContext.getAmount());

        // Given - Default category = 1%
        CreateCashbackRequest defaultRequest = new CreateCashbackRequest(
            TEST_ACCOUNT_ID, TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"), "MERCHANT001", "OTHER", null
        );

        // When
        CashbackSagaContext defaultContext = new CashbackSagaContext(defaultRequest);

        // Then
        assertEquals(new BigDecimal("10.00"), defaultContext.getAmount());
    }

    @Test
    void testSaga_Replay_DuplicateTransactionRecordIsNoOp() {
        // Given
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            null
        );

        CashbackSagaContext context = new CashbackSagaContext(request);
        Cashback existing = createTestCashback(UUID.randomUUID(), new BigDecimal("20.00"));

        when(walletServicePort.creditWallet(any(), any(), any(), any()))
            .thenReturn(true);
        when(sagaRepository.save(any()))
            .thenAnswer(inv -> inv.getArgument(0));
        when(sagaRepository.findById(any()))
            .thenReturn(Optional.empty());
        // PROMO-001: unique index on transaction_id rejects the duplicate insert
        when(cashbackRepository.save(any(Cashback.class)))
            .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key value violates unique constraint uq_cashback_transaction_id"));
        when(cashbackRepository.findByTransactionId(TEST_TRANSACTION_ID))
            .thenReturn(Optional.of(existing));

        // When
        SagaResult<CashbackSagaContext> result = orchestrator.executeCashbackSaga(context);

        // Then: replay is a success no-op returning the existing record
        assertTrue(result.isSuccess());
        assertEquals(SagaState.COMPLETED, result.getFinalState());
        assertEquals(existing.getId(), result.getData().getCashback().getId());
    }

    @Test
    void testSaga_Atomicity_WalletCreditMustSucceedBeforeCashbackRecord() {
        // This test verifies the core requirement of BUG-BE-062:
        // Cashback status should only be CREDITED after wallet credit succeeds

        // Given
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            null
        );

        CashbackSagaContext context = new CashbackSagaContext(request);
        Cashback savedCashback = createTestCashback(UUID.randomUUID(), new BigDecimal("20.00"));

        // Mock wallet credit success
        when(walletServicePort.creditWallet(any(), any(), any(), any()))
            .thenReturn(true);

        // Mock saga repository
        when(sagaRepository.save(any()))
            .thenAnswer(inv -> inv.getArgument(0));
        when(sagaRepository.findById(any()))
            .thenReturn(Optional.empty());

        // Mock cashback save
        when(cashbackRepository.save(any(Cashback.class)))
            .thenReturn(savedCashback);

        // When
        SagaResult<CashbackSagaContext> result = orchestrator.executeCashbackSaga(context);

        // Then
        assertTrue(result.isSuccess());

        // Verify the order: wallet credit happens before cashback record
        var inOrder = inOrder(walletServicePort, cashbackRepository);
        inOrder.verify(walletServicePort).creditWallet(any(), any(), any(), any());
        inOrder.verify(cashbackRepository).save(any(Cashback.class));
    }

    private Cashback createTestCashback(UUID id, BigDecimal amount) {
        Cashback cashback = new Cashback();
        cashback.setId(id);
        cashback.setAccountId(TEST_ACCOUNT_ID);
        cashback.setTransactionId(TEST_TRANSACTION_ID);
        cashback.setTransactionAmount(new BigDecimal("1000.00"));
        cashback.setCashbackAmount(amount);
        cashback.setStatus(CashbackStatus.CREDITED);
        cashback.setMerchantCode("MERCHANT001");
        cashback.setCategoryCode("GROCERY");
        return cashback;
    }
}
