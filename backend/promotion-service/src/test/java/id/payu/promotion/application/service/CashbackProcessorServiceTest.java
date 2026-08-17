package id.payu.promotion.application.service;

import id.payu.promotion.domain.model.*;
import id.payu.promotion.domain.port.out.CashbackRuleRepositoryPort;
import id.payu.promotion.domain.port.out.WalletServicePort;
import id.payu.promotion.domain.port.out.NotificationPort;
import id.payu.promotion.domain.port.out.CashbackRecordRepositoryPort;
import id.payu.promotion.interfaces.dto.TransactionCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Service tests for CashbackProcessorService (TDD - RED phase).
 * Tests the application service processing cashback on transaction completion.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CashbackProcessorService Tests")
class CashbackProcessorServiceTest {

    @Mock
    private CashbackRuleRepositoryPort cashbackRuleRepository;

    @Mock
    private CashbackRecordRepositoryPort cashbackRecordRepository;

    @Mock
    private WalletServicePort walletServicePort;

    @Mock
    private NotificationPort notificationPort;

    @InjectMocks
    private CashbackProcessorService cashbackProcessorService;

    private static final String ACCOUNT_ID = "acc-123";
    private static final String TRANSACTION_ID = "txn-456";
    private static final String MERCHANT_CODE = "MERCHANT001";
    private static final String CATEGORY_CODE = "GROCERY";

    @BeforeEach
    void setUp() {
        lenient().when(cashbackRecordRepository.findByTransactionIdAndRuleId(anyString(), anyString()))
                .thenReturn(java.util.Optional.empty());
    }

    @Test
    @DisplayName("should process cashback on transaction complete")
    void shouldProcessCashbackOnTransactionComplete() {
        // Given
        TransactionCompletedEvent event = createTransactionEvent(new BigDecimal("100000"));

        CashbackRule rule = createCashbackRule("RULE001", new BigDecimal("5000"), CashbackType.FIXED);

        when(cashbackRuleRepository.findActiveRules())
                .thenReturn(List.of(rule));
        when(cashbackRecordRepository.hasProcessedTransaction(TRANSACTION_ID))
                .thenReturn(false);
        when(walletServicePort.creditWallet(eq(ACCOUNT_ID), any(BigDecimal.class), anyString(), anyString()))
                .thenReturn(true);
        when(cashbackRecordRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CashbackResult result = cashbackProcessorService.process(event);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(1, result.getProcessedCount());
        assertEquals(new BigDecimal("5000"), result.getTotalCashbackAmount());

        // Verify wallet was credited
        verify(walletServicePort).creditWallet(
                eq(ACCOUNT_ID),
                argThat(bd -> bd.compareTo(new BigDecimal("5000")) == 0),
                anyString(),
                contains("CashbackEntity")
        );
    }

    @Test
    @DisplayName("should credit wallet for eligible cashback")
    void shouldCreditWalletForEligibleCashback() {
        // Given
        TransactionCompletedEvent event = createTransactionEvent(new BigDecimal("200000"));

        CashbackRule rule = createPercentageRule("RULE002", 5, new BigDecimal("10000"));

        when(cashbackRuleRepository.findActiveRules())
                .thenReturn(List.of(rule));
        when(cashbackRecordRepository.hasProcessedTransaction(TRANSACTION_ID))
                .thenReturn(false);
        when(walletServicePort.creditWallet(any(), any(), any(), any()))
                .thenReturn(true);
        when(cashbackRecordRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CashbackResult result = cashbackProcessorService.process(event);

        // Then - 5% of 200k = 10k, but capped at 10k max
        assertTrue(result.isSuccess());
        assertEquals(0, new BigDecimal("10000").compareTo(result.getTotalCashbackAmount()));

        verify(walletServicePort).creditWallet(
                eq(ACCOUNT_ID),
                argThat(bd -> bd.compareTo(new BigDecimal("10000")) == 0),
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("should send notification after credit")
    void shouldSendNotificationAfterCredit() {
        // Given
        TransactionCompletedEvent event = createTransactionEvent(new BigDecimal("100000"));

        CashbackRule rule = createCashbackRule("RULE003", new BigDecimal("5000"), CashbackType.FIXED);

        when(cashbackRuleRepository.findActiveRules())
                .thenReturn(List.of(rule));
        when(cashbackRecordRepository.hasProcessedTransaction(TRANSACTION_ID))
                .thenReturn(false);
        when(walletServicePort.creditWallet(any(), any(), any(), any()))
                .thenReturn(true);
        when(cashbackRecordRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        cashbackProcessorService.process(event);

        // Then
        ArgumentCaptor<CashbackNotification> notificationCaptor = ArgumentCaptor.forClass(CashbackNotification.class);
        verify(notificationPort).sendCashbackNotification(notificationCaptor.capture());

        CashbackNotification notification = notificationCaptor.getValue();
        assertEquals(ACCOUNT_ID, notification.getAccountId());
        assertEquals(TRANSACTION_ID, notification.getTransactionId());
        assertEquals(new BigDecimal("5000"), notification.getAmount());
    }

    @Test
    @DisplayName("should skip already processed transactions")
    void shouldSkipAlreadyProcessedTransactions() {
        // Given
        TransactionCompletedEvent event = createTransactionEvent(new BigDecimal("100000"));

        when(cashbackRecordRepository.hasProcessedTransaction(TRANSACTION_ID))
                .thenReturn(true);

        // When
        CashbackResult result = cashbackProcessorService.process(event);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(0, result.getProcessedCount());
        assertEquals(BigDecimal.ZERO, result.getTotalCashbackAmount());

        // Verify no rules were evaluated or wallet credited
        verify(cashbackRuleRepository, never()).findActiveRules();
        verify(walletServicePort, never()).creditWallet(any(), any(), any(), any());
    }

    @Test
    @DisplayName("should handle multiple matching rules")
    void shouldHandleMultipleMatchingRules() {
        // Given
        TransactionCompletedEvent event = createTransactionEvent(new BigDecimal("200000"));

        CashbackRule rule1 = createCashbackRule("RULE001", new BigDecimal("5000"), CashbackType.FIXED);
        CashbackRule rule2 = createPercentageRule("RULE002", 2, new BigDecimal("3000"));

        when(cashbackRuleRepository.findActiveRules())
                .thenReturn(List.of(rule1, rule2));
        when(cashbackRecordRepository.hasProcessedTransaction(TRANSACTION_ID))
                .thenReturn(false);
        when(walletServicePort.creditWallet(any(), any(), any(), any()))
                .thenReturn(true);
        when(cashbackRecordRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CashbackResult result = cashbackProcessorService.process(event);

        // Then - Should process both rules (5000 + 4000 = 9000, but rule2 capped at 3000 = 8000)
        assertTrue(result.isSuccess());
        assertEquals(2, result.getProcessedCount());
    }

    @Test
    @DisplayName("should persist cashback record before crediting wallet")
    void shouldPersistRecordBeforeCreditingWallet() {
        // Given
        TransactionCompletedEvent event = createTransactionEvent(new BigDecimal("100000"));
        CashbackRule rule = createCashbackRule("RULE001", new BigDecimal("5000"), CashbackType.FIXED);

        when(cashbackRuleRepository.findActiveRules()).thenReturn(List.of(rule));
        when(cashbackRecordRepository.hasProcessedTransaction(TRANSACTION_ID)).thenReturn(false);
        when(walletServicePort.creditWallet(any(), any(), any(), any())).thenReturn(true);
        when(cashbackRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        cashbackProcessorService.process(event);

        // Then - record must be persisted BEFORE money moves (PROMO-DOUBLE-001)
        InOrder inOrder = inOrder(cashbackRecordRepository, walletServicePort);
        inOrder.verify(cashbackRecordRepository).save(any());
        inOrder.verify(walletServicePort).creditWallet(any(), any(), any(), any());
    }

    @Test
    @DisplayName("should not swallow record save failure after wallet credit (rethrow for retry/DLQ)")
    void shouldRethrowWhenRecordSaveFailsAfterCredit() {
        // Given - wallet credit succeeds, but persisting the CREDITED record fails
        TransactionCompletedEvent event = createTransactionEvent(new BigDecimal("100000"));
        CashbackRule rule = createCashbackRule("RULE001", new BigDecimal("5000"), CashbackType.FIXED);

        when(cashbackRuleRepository.findActiveRules()).thenReturn(List.of(rule));
        when(cashbackRecordRepository.hasProcessedTransaction(TRANSACTION_ID)).thenReturn(false);
        when(walletServicePort.creditWallet(any(), any(), any(), any())).thenReturn(true);
        when(cashbackRecordRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0))
                .thenThrow(new RuntimeException("DB unavailable"));

        // When/Then - exception must propagate so the consumer retries instead of acking
        assertThrows(RuntimeException.class, () -> cashbackProcessorService.process(event));
        verify(notificationPort, never()).sendCashbackNotification(any());
    }

    @Test
    @DisplayName("should mark record CREDITED only after wallet credit succeeds")
    void shouldMarkRecordCreditedAfterWalletCredit() {
        // Given - record status is captured at each save invocation
        List<CashbackStatus> savedStatuses = new ArrayList<>();
        when(cashbackRuleRepository.findActiveRules()).thenReturn(List.of(
                createCashbackRule("RULE001", new BigDecimal("5000"), CashbackType.FIXED)));
        when(cashbackRecordRepository.hasProcessedTransaction(TRANSACTION_ID)).thenReturn(false);
        when(walletServicePort.creditWallet(any(), any(), any(), any())).thenReturn(true);
        when(cashbackRecordRepository.save(any())).thenAnswer(invocation -> {
            CashbackRecord record = invocation.getArgument(0);
            savedStatuses.add(record.getStatus());
            return record;
        });

        // When
        cashbackProcessorService.process(createTransactionEvent(new BigDecimal("100000")));

        // Then - intent persisted as PENDING, finalized as CREDITED
        assertEquals(List.of(CashbackStatus.PENDING, CashbackStatus.CREDITED), savedStatuses);
    }

    @Test
    @DisplayName("should mark record FAILED when wallet rejects the credit")
    void shouldMarkRecordFailedWhenWalletRejects() {
        // Given - wallet rejects the credit
        List<CashbackStatus> savedStatuses = new ArrayList<>();
        when(cashbackRuleRepository.findActiveRules()).thenReturn(List.of(
                createCashbackRule("RULE001", new BigDecimal("5000"), CashbackType.FIXED)));
        when(cashbackRecordRepository.hasProcessedTransaction(TRANSACTION_ID)).thenReturn(false);
        when(walletServicePort.creditWallet(any(), any(), any(), any())).thenReturn(false);
        when(cashbackRecordRepository.save(any())).thenAnswer(invocation -> {
            CashbackRecord record = invocation.getArgument(0);
            savedStatuses.add(record.getStatus());
            return record;
        });

        // When
        CashbackResult result = cashbackProcessorService.process(createTransactionEvent(new BigDecimal("100000")));

        // Then - no money moved, record FAILED, no notification, failure returned
        assertEquals(List.of(CashbackStatus.PENDING, CashbackStatus.FAILED), savedStatuses);
        assertFalse(result.isSuccess());
        verify(notificationPort, never()).sendCashbackNotification(any());
    }

    @Test
    @DisplayName("should filter rules by merchant code")
    void shouldFilterRulesByMerchantCode() {
        // Given
        TransactionCompletedEvent event = createTransactionEvent(
                new BigDecimal("100000"), "SPECIFIC_MERCHANT", "GROCERY"
        );

        CashbackRule generalRule = createCashbackRule("RULE_GENERAL", new BigDecimal("1000"), CashbackType.FIXED);
        CashbackRule specificRule = CashbackRule.builder()
                .ruleId("RULE_SPECIFIC")
                .name("Specific Merchant Rule")
                .cashbackType(CashbackType.FIXED)
                .cashbackAmount(new BigDecimal("5000"))
                .applicableMerchantCodes(java.util.Set.of("SPECIFIC_MERCHANT"))
                .build();

        when(cashbackRuleRepository.findActiveRules())
                .thenReturn(List.of(generalRule, specificRule));
        when(cashbackRecordRepository.hasProcessedTransaction(TRANSACTION_ID))
                .thenReturn(false);
        when(walletServicePort.creditWallet(any(), any(), any(), any()))
                .thenReturn(true);
        when(cashbackRecordRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CashbackResult result = cashbackProcessorService.process(event);

        // Then - Both rules should match, but specific rule only for specific merchant
        assertTrue(result.isSuccess());
        assertEquals(2, result.getProcessedCount());
    }

    @Test
    @DisplayName("should filter rules by category code")
    void shouldFilterRulesByCategoryCode() {
        // Given
        TransactionCompletedEvent event = createTransactionEvent(
                new BigDecimal("100000"), MERCHANT_CODE, "DINING"
        );

        CashbackRule groceryRule = CashbackRule.builder()
                .ruleId("RULE_GROCERY")
                .name("Grocery Rule")
                .cashbackType(CashbackType.FIXED)
                .cashbackAmount(new BigDecimal("5000"))
                .applicableCategories(java.util.Set.of("GROCERY"))
                .build();

        CashbackRule diningRule = CashbackRule.builder()
                .ruleId("RULE_DINING")
                .name("Dining Rule")
                .cashbackType(CashbackType.FIXED)
                .cashbackAmount(new BigDecimal("3000"))
                .applicableCategories(java.util.Set.of("DINING"))
                .build();

        when(cashbackRuleRepository.findActiveRules())
                .thenReturn(List.of(groceryRule, diningRule));
        when(cashbackRecordRepository.hasProcessedTransaction(TRANSACTION_ID))
                .thenReturn(false);
        when(walletServicePort.creditWallet(any(), any(), any(), any()))
                .thenReturn(true);
        when(cashbackRecordRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CashbackResult result = cashbackProcessorService.process(event);

        // Then - Only dining rule should match
        assertTrue(result.isSuccess());
        assertEquals(1, result.getProcessedCount());
        assertEquals(new BigDecimal("3000"), result.getTotalCashbackAmount());
    }

    private TransactionCompletedEvent createTransactionEvent(BigDecimal amount) {
        return createTransactionEvent(amount, MERCHANT_CODE, CATEGORY_CODE);
    }

    private TransactionCompletedEvent createTransactionEvent(BigDecimal amount, String merchantCode, String categoryCode) {
        return TransactionCompletedEvent.builder()
                .transactionId(TRANSACTION_ID)
                .accountId(ACCOUNT_ID)
                .amount(amount)
                .merchantCode(merchantCode)
                .categoryCode(categoryCode)
                .timestamp(Instant.now())
                .build();
    }

    private CashbackRule createCashbackRule(String ruleId, BigDecimal amount, CashbackType type) {
        return CashbackRule.builder()
                .ruleId(ruleId)
                .name("Test Rule " + ruleId)
                .cashbackType(type)
                .cashbackAmount(amount)
                .minAmount(new BigDecimal("10000"))
                .active(true)
                .build();
    }

    private CashbackRule createPercentageRule(String ruleId, double percentage, BigDecimal maxCashback) {
        return CashbackRule.builder()
                .ruleId(ruleId)
                .name("Test Percentage Rule " + ruleId)
                .cashbackType(CashbackType.PERCENTAGE)
                .cashbackPercentage(BigDecimal.valueOf(percentage))
                .maxCashback(maxCashback)
                .minAmount(new BigDecimal("10000"))
                .active(true)
                .build();
    }
}
