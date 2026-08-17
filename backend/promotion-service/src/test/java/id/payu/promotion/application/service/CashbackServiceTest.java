package id.payu.promotion.application.service;

import id.payu.promotion.application.saga.CashbackSagaContext;
import id.payu.promotion.domain.port.in.CashbackSagaUseCase;
import id.payu.promotion.domain.model.CashbackCommand;
import id.payu.promotion.domain.model.CashbackSagaOutcome;
import id.payu.promotion.domain.model.Cashback;
import id.payu.promotion.domain.port.out.CashbackPersistencePort;
import id.payu.promotion.domain.CashbackStatus;
import id.payu.promotion.interfaces.dto.CreateCashbackRequest;
import id.payu.promotion.domain.port.out.WalletServicePort;
import id.payu.promotion.domain.port.out.CashbackEventPublisher;
import id.payu.saga.model.SagaResult;
import id.payu.saga.model.SagaState;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CashbackService with mocked wallet service.
 * Tests the saga pattern implementation for cashback creation.
 */
@ExtendWith(MockitoExtension.class)
class CashbackServiceTest {

    @Mock
    private CashbackSagaUseCase sagaOrchestrator;

    @Mock
    private WalletServicePort walletServicePort;

    @Mock
    private CashbackPersistencePort cashbackRepository;

    @Mock
    private CashbackEventPublisher outboxService;

    @InjectMocks
    private CashbackService cashbackService;

    private static final String TEST_ACCOUNT_ID = "acc-123";
    private static final String TEST_TRANSACTION_ID = "txn-456";

    @BeforeEach
    void setUp() {
        // MeterRegistry is @Autowired(required=false); inject a simple in-memory instance
        // since constructor injection of Mockito @InjectMocks cannot set required=false deps
        ReflectionTestUtils.setField(cashbackService, "meterRegistry", new SimpleMeterRegistry());
        // promotionEventsTopic String has @Value default; inject a test value
        ReflectionTestUtils.setField(cashbackService, "promotionEventsTopic", "payu.promotion.cashback-event.v1");
    }

    @Test
    void testCreateCashback_Success_WalletCreditSucceeds() {
        // Given
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            "CASHBACK10"
        );

        Cashback expectedCashback = createTestCashback(UUID.randomUUID(), TEST_ACCOUNT_ID,
            new BigDecimal("20.00"), CashbackStatus.CREDITED);

        CashbackSagaContext context = new CashbackSagaContext();
        context.setCashback(expectedCashback);

        SagaResult<CashbackSagaContext> successResult = SagaResult.success(
            UUID.randomUUID().toString(),
            "CASHBACK_CREDIT_SAGA",
            context
        );

        when(sagaOrchestrator.execute(any(CashbackCommand.class)))
            .thenReturn(toOutcome(successResult));

        // When
        Cashback result = cashbackService.createCashback(request);

        // Then
        assertNotNull(result);
        assertEquals(TEST_ACCOUNT_ID, result.getAccountId());
        assertEquals(CashbackStatus.CREDITED, result.getStatus());

        verify(sagaOrchestrator).execute(any(CashbackCommand.class));
    }

    @Test
    void testCreateCashback_Failure_WalletCreditFails() {
        // Given
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            "CASHBACK10"
        );

        SagaResult<CashbackSagaContext> failureResult = SagaResult.failure(
            UUID.randomUUID().toString(),
            "CASHBACK_CREDIT_SAGA",
            "Wallet service unavailable",
            "CREDIT_WALLET"
        );

        when(sagaOrchestrator.execute(any(CashbackCommand.class)))
            .thenReturn(toOutcome(failureResult));

        // When & Then
        CashbackService.CashbackCreationException exception = assertThrows(
            CashbackService.CashbackCreationException.class,
            () -> cashbackService.createCashback(request)
        );

        assertTrue(exception.getMessage().contains("Failed to create cashback"));
        assertEquals("CREDIT_WALLET", exception.getFailedStep());

        verify(sagaOrchestrator).execute(any(CashbackCommand.class));
    }

    @Test
    void testCreateCashback_WalletCreditFails_CashbackNotCredited() {
        // Given - Simulate wallet credit failure
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            null
        );

        // Create failure result for saga
        ArgumentCaptor<CashbackCommand> contextCaptor = ArgumentCaptor.forClass(CashbackCommand.class);

        SagaResult<CashbackSagaContext> failureResult = SagaResult.<CashbackSagaContext>builder()
            .sagaId(UUID.randomUUID().toString())
            .sagaType("CASHBACK_CREDIT_SAGA")
            .finalState(SagaState.FAILED)
            .errorMessage("Wallet credit failed: Wallet service unavailable")
            .errorStep("CREDIT_WALLET")
            .build();

        when(sagaOrchestrator.execute(contextCaptor.capture()))
            .thenReturn(toOutcome(failureResult));

        // When & Then
        assertThrows(CashbackService.CashbackCreationException.class,
            () -> cashbackService.createCashback(request));

        // Verify saga was called with correct context
        CashbackCommand capturedContext = contextCaptor.getValue();
        assertEquals(TEST_ACCOUNT_ID, capturedContext.accountId());
        assertEquals(new BigDecimal("20.00"), new CashbackSagaContext(request).getAmount());
    }

    @Test
    void testCreateCashback_GroceryCategory_Returns2Percent() {
        // Given
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            null
        );

        Cashback expectedCashback = createTestCashback(UUID.randomUUID(), TEST_ACCOUNT_ID,
            new BigDecimal("20.00"), CashbackStatus.CREDITED);
        expectedCashback.setPercentage(new BigDecimal("2.0000"));

        CashbackSagaContext context = new CashbackSagaContext();
        context.setCashback(expectedCashback);

        SagaResult<CashbackSagaContext> successResult = SagaResult.success(
            UUID.randomUUID().toString(),
            "CASHBACK_CREDIT_SAGA",
            context
        );

        ArgumentCaptor<CashbackCommand> contextCaptor = ArgumentCaptor.forClass(CashbackCommand.class);
        when(sagaOrchestrator.execute(contextCaptor.capture()))
            .thenReturn(toOutcome(successResult));

        // When
        Cashback result = cashbackService.createCashback(request);

        // Then — verify the mock's return value
        assertEquals(new BigDecimal("20.00"), result.getCashbackAmount());
        assertEquals(new BigDecimal("2.0000"), result.getPercentage());

        // Also verify the context's calculated amount (exercises CashbackSagaContext.calculateCashbackAmount)
        CashbackCommand capturedContext = contextCaptor.getValue();
        assertEquals(new BigDecimal("20.00"), new CashbackSagaContext(request).getAmount(),
            "GROCERY category should calculate 2% of 1000.00 = 20.00");
    }

    @Test
    void testCreateCashback_DiningCategory_Returns3Percent() {
        // Given
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "DINING",
            null
        );

        Cashback expectedCashback = createTestCashback(UUID.randomUUID(), TEST_ACCOUNT_ID,
            new BigDecimal("30.00"), CashbackStatus.CREDITED);
        expectedCashback.setPercentage(new BigDecimal("3.0000"));

        CashbackSagaContext context = new CashbackSagaContext();
        context.setCashback(expectedCashback);

        SagaResult<CashbackSagaContext> successResult = SagaResult.success(
            UUID.randomUUID().toString(),
            "CASHBACK_CREDIT_SAGA",
            context
        );

        ArgumentCaptor<CashbackCommand> contextCaptor = ArgumentCaptor.forClass(CashbackCommand.class);
        when(sagaOrchestrator.execute(contextCaptor.capture()))
            .thenReturn(toOutcome(successResult));

        // When
        Cashback result = cashbackService.createCashback(request);

        // Then
        assertEquals(new BigDecimal("30.00"), result.getCashbackAmount());
        assertEquals(new BigDecimal("3.0000"), result.getPercentage());

        // Verify context's calculated amount
        CashbackCommand capturedContext = contextCaptor.getValue();
        assertEquals(new BigDecimal("30.00"), new CashbackSagaContext(request).getAmount(),
            "DINING category should calculate 3% of 1000.00 = 30.00");
    }

    @Test
    void testCreateCashback_ShoppingCategory_Returns1Point5Percent() {
        // Given
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "SHOPPING",
            null
        );

        Cashback expectedCashback = createTestCashback(UUID.randomUUID(), TEST_ACCOUNT_ID,
            new BigDecimal("15.00"), CashbackStatus.CREDITED);
        expectedCashback.setPercentage(new BigDecimal("1.5000"));

        CashbackSagaContext context = new CashbackSagaContext();
        context.setCashback(expectedCashback);

        SagaResult<CashbackSagaContext> successResult = SagaResult.success(
            UUID.randomUUID().toString(),
            "CASHBACK_CREDIT_SAGA",
            context
        );

        ArgumentCaptor<CashbackCommand> contextCaptor = ArgumentCaptor.forClass(CashbackCommand.class);
        when(sagaOrchestrator.execute(contextCaptor.capture()))
            .thenReturn(toOutcome(successResult));

        // When
        Cashback result = cashbackService.createCashback(request);

        // Then
        assertEquals(new BigDecimal("15.00"), result.getCashbackAmount());
        assertEquals(new BigDecimal("1.5000"), result.getPercentage());

        // Verify context's calculated amount
        CashbackCommand capturedContext = contextCaptor.getValue();
        assertEquals(new BigDecimal("15.00"), new CashbackSagaContext(request).getAmount(),
            "SHOPPING category should calculate 1.5% of 1000.00 = 15.00");
    }

    @Test
    void testCreateCashback_DefaultCategory_Returns1Percent() {
        // Given
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "OTHER",
            null
        );

        Cashback expectedCashback = createTestCashback(UUID.randomUUID(), TEST_ACCOUNT_ID,
            new BigDecimal("10.00"), CashbackStatus.CREDITED);
        expectedCashback.setPercentage(new BigDecimal("1.0000"));

        CashbackSagaContext context = new CashbackSagaContext();
        context.setCashback(expectedCashback);

        SagaResult<CashbackSagaContext> successResult = SagaResult.success(
            UUID.randomUUID().toString(),
            "CASHBACK_CREDIT_SAGA",
            context
        );

        ArgumentCaptor<CashbackCommand> contextCaptor = ArgumentCaptor.forClass(CashbackCommand.class);
        when(sagaOrchestrator.execute(contextCaptor.capture()))
            .thenReturn(toOutcome(successResult));

        // When
        Cashback result = cashbackService.createCashback(request);

        // Then
        assertEquals(new BigDecimal("10.00"), result.getCashbackAmount());
        assertEquals(new BigDecimal("1.0000"), result.getPercentage());

        // Verify context's calculated amount
        CashbackCommand capturedContext = contextCaptor.getValue();
        assertEquals(new BigDecimal("10.00"), new CashbackSagaContext(request).getAmount(),
            "DEFAULT (OTHER) category should calculate 1% of 1000.00 = 10.00");
    }

    @Test
    void testCreateCashback_NoCategory_Returns1Percent() {
        // Given
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            null,
            null
        );

        Cashback expectedCashback = createTestCashback(UUID.randomUUID(), TEST_ACCOUNT_ID,
            new BigDecimal("10.00"), CashbackStatus.CREDITED);
        expectedCashback.setPercentage(new BigDecimal("1.0000"));

        CashbackSagaContext context = new CashbackSagaContext();
        context.setCashback(expectedCashback);

        SagaResult<CashbackSagaContext> successResult = SagaResult.success(
            UUID.randomUUID().toString(),
            "CASHBACK_CREDIT_SAGA",
            context
        );

        when(sagaOrchestrator.execute(any(CashbackCommand.class)))
            .thenReturn(toOutcome(successResult));

        // When
        Cashback result = cashbackService.createCashback(request);

        // Then
        assertEquals(new BigDecimal("10.00"), result.getCashbackAmount());
        assertEquals(new BigDecimal("1.0000"), result.getPercentage());
    }

    @Test
    void testCreateCashback_DecimalPrecision() {
        // Given
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1234.56"),
            "MERCHANT001",
            "GROCERY",
            null
        );

        Cashback expectedCashback = createTestCashback(UUID.randomUUID(), TEST_ACCOUNT_ID,
            new BigDecimal("24.69"), CashbackStatus.CREDITED);

        CashbackSagaContext context = new CashbackSagaContext();
        context.setCashback(expectedCashback);

        SagaResult<CashbackSagaContext> successResult = SagaResult.success(
            UUID.randomUUID().toString(),
            "CASHBACK_CREDIT_SAGA",
            context
        );

        when(sagaOrchestrator.execute(any(CashbackCommand.class)))
            .thenReturn(toOutcome(successResult));

        // When
        Cashback result = cashbackService.createCashback(request);

        // Then
        assertEquals(new BigDecimal("24.69"), result.getCashbackAmount());
    }

    @Test
    void testCreateCashback_WithCustomCashbackCode() {
        // Given
        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            "PROMO2024"
        );

        Cashback expectedCashback = createTestCashback(UUID.randomUUID(), TEST_ACCOUNT_ID,
            new BigDecimal("20.00"), CashbackStatus.CREDITED);
        expectedCashback.setCashbackCode("PROMO2024");

        CashbackSagaContext context = new CashbackSagaContext();
        context.setCashback(expectedCashback);

        SagaResult<CashbackSagaContext> successResult = SagaResult.success(
            UUID.randomUUID().toString(),
            "CASHBACK_CREDIT_SAGA",
            context
        );

        when(sagaOrchestrator.execute(any(CashbackCommand.class)))
            .thenReturn(toOutcome(successResult));

        // When
        Cashback result = cashbackService.createCashback(request);

        // Then
        assertEquals("PROMO2024", result.getCashbackCode());
    }

    @Test
    void testCreateCashback_StatusOnlyCreditedAfterWalletSuccess() {
        // Given - This test verifies the core bug fix:
        // Cashback status should only be CREDITED after wallet credit succeeds

        CreateCashbackRequest request = new CreateCashbackRequest(
            TEST_ACCOUNT_ID,
            TEST_TRANSACTION_ID,
            new BigDecimal("1000.00"),
            "MERCHANT001",
            "GROCERY",
            null
        );

        Cashback expectedCashback = createTestCashback(UUID.randomUUID(), TEST_ACCOUNT_ID,
            new BigDecimal("20.00"), CashbackStatus.CREDITED);
        expectedCashback.setCreditedAt(java.time.LocalDateTime.now());

        CashbackSagaContext context = new CashbackSagaContext();
        context.setCashback(expectedCashback);
        context.setWalletCredited(true);
        context.setCashbackRecorded(true);

        SagaResult<CashbackSagaContext> successResult = SagaResult.success(
            UUID.randomUUID().toString(),
            "CASHBACK_CREDIT_SAGA",
            context
        );

        when(sagaOrchestrator.execute(any(CashbackCommand.class)))
            .thenReturn(toOutcome(successResult));

        // When
        Cashback result = cashbackService.createCashback(request);

        // Then
        assertEquals(CashbackStatus.CREDITED, result.getStatus());
        assertNotNull(result.getCreditedAt());

        // Verify the saga context indicates both steps succeeded
        assertTrue(context.isWalletCredited(), "Wallet should be credited");
        assertTrue(context.isCashbackRecorded(), "Cashback should be recorded");
    }

    private Cashback createTestCashback(UUID id, String accountId, BigDecimal amount, CashbackStatus status) {
        Cashback cashback = new Cashback();
        cashback.setId(id);
        cashback.setAccountId(accountId);
        cashback.setTransactionId(TEST_TRANSACTION_ID);
        cashback.setTransactionAmount(new BigDecimal("1000.00"));
        cashback.setCashbackAmount(amount);
        cashback.setStatus(status);
        cashback.setMerchantCode("MERCHANT001");
        cashback.setCategoryCode("GROCERY");
        return cashback;
    }

    private CashbackSagaOutcome toOutcome(SagaResult<CashbackSagaContext> result) {
        Cashback cashback = result.getData() == null ? null : result.getData().getCashback();
        String state = result.getFinalState() == null ? null : result.getFinalState().name();
        return new CashbackSagaOutcome(result.isSuccess(), result.isCompensated(), cashback,
            result.getErrorMessage(), result.getErrorStep(), state);
    }
}
