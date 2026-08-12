package id.payu.lending.application.service;

import id.payu.lending.adapter.external.AccountClient;
import id.payu.lending.adapter.external.TransactionClient;
import id.payu.lending.dto.TransactionSummaryResponse;
import id.payu.lending.dto.UserResponse;
import id.payu.rules.service.RulesEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnhancedCreditScoringService Unit Tests")
class EnhancedCreditScoringServiceTest {

    @Mock
    private AccountClient accountClient;

    @Mock
    private TransactionClient transactionClient;

    @Mock
    private id.payu.lending.adapter.client.AccountGrpcClient accountGrpcClient;

    private RulesEngineService rulesEngineService;
    private EnhancedCreditScoringService service;

    @BeforeEach
    void setUp() {
        rulesEngineService = new RulesEngineService();
        service = new EnhancedCreditScoringService(accountClient, accountGrpcClient, transactionClient, rulesEngineService);
    }

    @Test
    @DisplayName("Should calculate credit score using Drools rules for APPROVED KYC and 36+ Months tenure")
    void testCalculateEnhancedCreditScore_approvedKyc_longTenure() {
        UUID userId = UUID.randomUUID();
        BigDecimal baseScore = new BigDecimal("500");

        // Approved KYC: +50
        // Tenure 37 months: +40
        // Total Transactions 150: +10
        // Total Amount 150M: +20
        // Success rate 149/150 = 0.9933 (>= 0.98): +30
        // Total expected addition: 150
        // Base score 500 + 150 = 650

        UserResponse user = new UserResponse(
                userId,
                "ext-1",
                "john_doe",
                "john@example.com",
                "08123456789",
                "John Doe",
                "1234567890123456",
                "ACTIVE",
                "APPROVED",
                LocalDateTime.now().minusYears(3).minusMonths(1)
        );

        TransactionSummaryResponse summary = new TransactionSummaryResponse(
                userId,
                150,
                new BigDecimal("150000000"),
                new BigDecimal("100000000"),
                new BigDecimal("50000000"),
                149,
                1,
                java.time.Instant.now().minus(100, java.time.temporal.ChronoUnit.DAYS),
                java.time.Instant.now()
        );

        when(accountClient.getUserById(userId.toString()))
                .thenReturn(id.payu.api.common.response.ApiResponse.success(user));
        when(accountGrpcClient.getAccountIdsByUserId(userId.toString()))
                .thenReturn(java.util.List.of(userId));
        when(transactionClient.getTransactionSummary(userId))
                .thenReturn(id.payu.api.common.response.ApiResponse.success(summary));

        BigDecimal score = service.calculateEnhancedCreditScore(userId, baseScore);
        assertEquals(0, new BigDecimal("650").compareTo(score), "Expected credit score to be 650");
    }

    @Test
    @DisplayName("Should calculate credit score for PENDING KYC and short tenure")
    void testCalculateEnhancedCreditScore_pendingKyc_shortTenure() {
        UUID userId = UUID.randomUUID();
        BigDecimal baseScore = new BigDecimal("400");

        // Pending KYC: +25
        // Tenure 8 months: +10
        // Total Transactions 75: +5
        // Total Amount 75M: +15
        // Success rate 72 / 75 = 0.96 (>= 0.95): +20
        // Total expected addition: 75
        // Base score 400 + 75 = 475

        UserResponse user = new UserResponse(
                userId,
                "ext-2",
                "jane_doe",
                "jane@example.com",
                "08123456780",
                "Jane Doe",
                "1234567890123457",
                "ACTIVE",
                "PENDING",
                LocalDateTime.now().minusMonths(8)
        );

        TransactionSummaryResponse summary = new TransactionSummaryResponse(
                userId,
                75,
                new BigDecimal("75000000"),
                new BigDecimal("50000000"),
                new BigDecimal("25000000"),
                72,
                3,
                java.time.Instant.now().minus(50, java.time.temporal.ChronoUnit.DAYS),
                java.time.Instant.now()
        );

        when(accountClient.getUserById(userId.toString()))
                .thenReturn(id.payu.api.common.response.ApiResponse.success(user));
        when(accountGrpcClient.getAccountIdsByUserId(userId.toString()))
                .thenReturn(java.util.List.of(userId));
        when(transactionClient.getTransactionSummary(userId))
                .thenReturn(id.payu.api.common.response.ApiResponse.success(summary));

        BigDecimal score = service.calculateEnhancedCreditScore(userId, baseScore);
        assertEquals(0, new BigDecimal("475").compareTo(score), "Expected credit score to be 475");
    }
}
