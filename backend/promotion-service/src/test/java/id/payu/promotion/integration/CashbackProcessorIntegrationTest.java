package id.payu.promotion.integration;

import id.payu.promotion.application.service.CashbackProcessorService;
import id.payu.promotion.adapter.persistence.CashbackRecordPersistenceAdapter;
import id.payu.promotion.adapter.persistence.CashbackRulePersistenceAdapter;
import id.payu.promotion.domain.model.*;
import id.payu.promotion.domain.port.out.CashbackRuleRepositoryPort;
import id.payu.promotion.domain.port.out.WalletServicePort;
import id.payu.promotion.interfaces.dto.TransactionCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for CashbackEntity Processor feature.
 * Tests the complete flow from Kafka event through service to wallet credit.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("CashbackEntity Processor Integration Tests")
class CashbackProcessorIntegrationTest {

    @Autowired
    private CashbackProcessorService cashbackProcessorService;

    @Autowired
    private CashbackRuleRepositoryPort cashbackRuleRepository;

    @Autowired
    private CashbackRulePersistenceAdapter cashbackRulePersistenceAdapter;

    @Autowired
    private CashbackRecordPersistenceAdapter cashbackRecordPersistenceAdapter;

    @MockitoBean(name = "walletGrpcAdapter")
    private WalletServicePort walletServicePort;

    private static final String ACCOUNT_ID = "acc-123";
    private static final String TRANSACTION_ID = "txn-456";

    @BeforeEach
    void setUp() {
        cashbackRulePersistenceAdapter.clear();
        cashbackRecordPersistenceAdapter.clear();
        when(walletServicePort.creditWallet(any(), any(), any(), any()))
                .thenReturn(true);
    }

    @Test
    @DisplayName("should process cashback for matching fixed rule")
    void shouldProcessCashbackForFixedRule() {
        // Given
        CashbackRule rule = CashbackRule.builder()
                .ruleId("RULE001")
                .name("Fixed CashbackEntity")
                .cashbackType(CashbackType.FIXED)
                .cashbackAmount(new BigDecimal("5000"))
                .minAmount(new BigDecimal("50000"))
                .active(true)
                .build();
        cashbackRuleRepository.save(rule);

        TransactionCompletedEvent event = TransactionCompletedEvent.builder()
                .transactionId(TRANSACTION_ID)
                .accountId(ACCOUNT_ID)
                .amount(new BigDecimal("100000"))
                .merchantCode("MERCHANT001")
                .categoryCode("GROCERY")
                .build();

        // When
        CashbackResult result = cashbackProcessorService.process(event);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(1, result.getProcessedCount());
        assertEquals(0, new BigDecimal("5000").compareTo(result.getTotalCashbackAmount()));

        verify(walletServicePort).creditWallet(
                eq(ACCOUNT_ID),
                argThat(amount -> new BigDecimal("5000").compareTo(amount) == 0),
                contains(TRANSACTION_ID),
                anyString()
        );
    }

    @Test
    @DisplayName("should process cashback for matching percentage rule")
    void shouldProcessCashbackForPercentageRule() {
        // Given
        CashbackRule rule = CashbackRule.builder()
                .ruleId("RULE002")
                .name("Percentage CashbackEntity")
                .cashbackType(CashbackType.PERCENTAGE)
                .cashbackPercentage(new BigDecimal("5"))
                .maxCashback(new BigDecimal("10000"))
                .minAmount(new BigDecimal("10000"))
                .active(true)
                .build();
        cashbackRuleRepository.save(rule);

        TransactionCompletedEvent event = TransactionCompletedEvent.builder()
                .transactionId(TRANSACTION_ID)
                .accountId(ACCOUNT_ID)
                .amount(new BigDecimal("200000"))
                .merchantCode("MERCHANT001")
                .categoryCode("DINING")
                .build();

        // When
        CashbackResult result = cashbackProcessorService.process(event);

        // Then - 5% of 200k = 10k, capped at 10k max
        assertTrue(result.isSuccess());
        assertEquals(1, result.getProcessedCount());
        assertEquals(0, new BigDecimal("10000").compareTo(result.getTotalCashbackAmount()));
    }

    @Test
    @DisplayName("should skip non-matching transactions")
    void shouldSkipNonMatchingTransactions() {
        // Given
        CashbackRule rule = CashbackRule.builder()
                .ruleId("RULE003")
                .name("High Minimum")
                .cashbackType(CashbackType.FIXED)
                .cashbackAmount(new BigDecimal("5000"))
                .minAmount(new BigDecimal("100000"))
                .active(true)
                .build();
        cashbackRuleRepository.save(rule);

        TransactionCompletedEvent event = TransactionCompletedEvent.builder()
                .transactionId(TRANSACTION_ID)
                .accountId(ACCOUNT_ID)
                .amount(new BigDecimal("50000")) // Below minimum
                .merchantCode("MERCHANT001")
                .categoryCode("GROCERY")
                .build();

        // When
        CashbackResult result = cashbackProcessorService.process(event);

        // Then
        assertTrue(result.isSuccess()); // Success but no cashback
        assertEquals(0, result.getProcessedCount());
        assertEquals(BigDecimal.ZERO, result.getTotalCashbackAmount());

        verify(walletServicePort, never()).creditWallet(any(), any(), any(), any());
    }

    @Test
    @DisplayName("should skip already processed transactions")
    void shouldSkipAlreadyProcessedTransactions() {
        // Given
        CashbackRule rule = CashbackRule.builder()
                .ruleId("RULE004")
                .name("Fixed CashbackEntity")
                .cashbackType(CashbackType.FIXED)
                .cashbackAmount(new BigDecimal("5000"))
                .minAmount(new BigDecimal("10000"))
                .active(true)
                .build();
        cashbackRuleRepository.save(rule);

        TransactionCompletedEvent event = TransactionCompletedEvent.builder()
                .transactionId(TRANSACTION_ID)
                .accountId(ACCOUNT_ID)
                .amount(new BigDecimal("100000"))
                .build();

        // When - First processing
        cashbackProcessorService.process(event);

        // When - Second processing (should be skipped)
        CashbackResult result = cashbackProcessorService.process(event);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(0, result.getProcessedCount());

        // Wallet should only be called once
        verify(walletServicePort, times(1)).creditWallet(any(), any(), any(), any());
    }

    @Test
    @DisplayName("should filter by merchant code")
    void shouldFilterByMerchantCode() {
        // Given
        CashbackRule rule = CashbackRule.builder()
                .ruleId("RULE005")
                .name("Specific Merchant")
                .cashbackType(CashbackType.FIXED)
                .cashbackAmount(new BigDecimal("5000"))
                .minAmount(new BigDecimal("10000"))
                .applicableMerchantCodes(java.util.Set.of("SPECIFIC_MERCHANT"))
                .active(true)
                .build();
        cashbackRuleRepository.save(rule);

        TransactionCompletedEvent matchingEvent = TransactionCompletedEvent.builder()
                .transactionId(TRANSACTION_ID + "1")
                .accountId(ACCOUNT_ID)
                .amount(new BigDecimal("100000"))
                .merchantCode("SPECIFIC_MERCHANT")
                .build();

        TransactionCompletedEvent nonMatchingEvent = TransactionCompletedEvent.builder()
                .transactionId(TRANSACTION_ID + "2")
                .accountId(ACCOUNT_ID)
                .amount(new BigDecimal("100000"))
                .merchantCode("OTHER_MERCHANT")
                .build();

        // When
        CashbackResult result1 = cashbackProcessorService.process(matchingEvent);
        CashbackResult result2 = cashbackProcessorService.process(nonMatchingEvent);

        // Then
        assertEquals(1, result1.getProcessedCount());
        assertEquals(0, result2.getProcessedCount());
    }
}
