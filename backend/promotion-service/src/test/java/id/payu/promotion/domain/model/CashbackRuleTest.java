package id.payu.promotion.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Domain tests for CashbackRule entity (TDD - RED phase first).
 * Tests the rich domain model behavior for cashback auto-application.
 */
@DisplayName("CashbackRule Domain Tests")
class CashbackRuleTest {

    private Transaction createTransaction(BigDecimal amount) {
        return createTransaction(amount, "MERCHANT001", "GROCERY");
    }

    private Transaction createTransaction(BigDecimal amount, String merchantCode, String categoryCode) {
        return Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .accountId("acc-123")
                .amount(amount)
                .merchantCode(merchantCode)
                .categoryCode(categoryCode)
                .timestamp(Instant.now())
                .build();
    }

    @Test
    @DisplayName("should match transaction by minimum amount")
    void shouldMatchTransactionByAmount() {
        CashbackRule rule = CashbackRule.builder()
                .ruleId("RULE001")
                .name("Min 50k CashbackEntity")
                .minAmount(new BigDecimal("50000"))
                .cashbackAmount(new BigDecimal("5000"))
                .cashbackType(CashbackType.FIXED)
                .build();

        Transaction txn = createTransaction(new BigDecimal("75000"));

        boolean matches = rule.matches(txn);
        BigDecimal cashback = rule.calculateCashback(txn);

        assertTrue(matches, "Transaction should match the rule");
        assertEquals(new BigDecimal("5000"), cashback, "CashbackEntity should be fixed 5000");
    }

    @Test
    @DisplayName("should not match transaction below threshold")
    void shouldNotMatchBelowThreshold() {
        CashbackRule rule = CashbackRule.builder()
                .ruleId("RULE002")
                .name("Min 100k CashbackEntity")
                .minAmount(new BigDecimal("100000"))
                .cashbackAmount(new BigDecimal("10000"))
                .cashbackType(CashbackType.FIXED)
                .build();

        Transaction txn = createTransaction(new BigDecimal("50000"));

        boolean matches = rule.matches(txn);

        assertFalse(matches, "Transaction should not match the rule");
    }

    @Test
    @DisplayName("should calculate percentage cashback correctly")
    void shouldCalculatePercentageCashback() {
        CashbackRule rule = CashbackRule.builder()
                .ruleId("RULE003")
                .name("5% CashbackEntity")
                .cashbackPercentage(new BigDecimal("5"))
                .cashbackType(CashbackType.PERCENTAGE)
                .maxCashback(new BigDecimal("10000"))
                .build();

        Transaction txn = createTransaction(new BigDecimal("200000"));

        boolean matches = rule.matches(txn);
        BigDecimal cashback = rule.calculateCashback(txn);

        // 5% of 200k = 10k
        assertTrue(matches);
        assertEquals(0, new BigDecimal("10000").compareTo(cashback), "5% of 200k should be 10k");
    }

    @Test
    @DisplayName("should respect maximum cashback cap for percentage")
    void shouldRespectMaxCashbackCap() {
        CashbackRule rule = CashbackRule.builder()
                .ruleId("RULE004")
                .name("10% CashbackEntity Capped")
                .cashbackPercentage(new BigDecimal("10"))
                .cashbackType(CashbackType.PERCENTAGE)
                .maxCashback(new BigDecimal("5000"))
                .build();

        Transaction txn = createTransaction(new BigDecimal("200000"));

        BigDecimal cashback = rule.calculateCashback(txn);

        assertEquals(new BigDecimal("5000"), cashback, "Should be capped at max cashback");
    }

    @Test
    @DisplayName("should match by merchant code")
    void shouldMatchByMerchantCode() {
        CashbackRule rule = CashbackRule.builder()
                .ruleId("RULE005")
                .name("Specific Merchant")
                .minAmount(new BigDecimal("10000"))
                .cashbackAmount(new BigDecimal("1000"))
                .cashbackType(CashbackType.FIXED)
                .applicableMerchantCodes(java.util.Set.of("MERCHANT001", "MERCHANT002"))
                .build();

        Transaction matchingTxn = createTransaction(new BigDecimal("50000"), "MERCHANT001", "GROCERY");
        Transaction nonMatchingTxn = createTransaction(new BigDecimal("50000"), "MERCHANT999", "GROCERY");

        assertTrue(rule.matches(matchingTxn), "Should match specific merchant");
        assertFalse(rule.matches(nonMatchingTxn), "Should not match different merchant");
    }

    @Test
    @DisplayName("should match by category code")
    void shouldMatchByCategoryCode() {
        CashbackRule rule = CashbackRule.builder()
                .ruleId("RULE006")
                .name("Grocery Category")
                .minAmount(new BigDecimal("10000"))
                .cashbackAmount(new BigDecimal("2000"))
                .cashbackType(CashbackType.FIXED)
                .applicableCategories(java.util.Set.of("GROCERY", "SUPERMARKET"))
                .build();

        Transaction matchingTxn = createTransaction(new BigDecimal("50000"), "MERCHANT001", "GROCERY");
        Transaction nonMatchingTxn = createTransaction(new BigDecimal("50000"), "MERCHANT001", "ELECTRONICS");

        assertTrue(rule.matches(matchingTxn), "Should match grocery category");
        assertFalse(rule.matches(nonMatchingTxn), "Should not match electronics category");
    }

    @Test
    @DisplayName("should not match expired rule")
    void shouldNotMatchExpiredRule() {
        CashbackRule rule = CashbackRule.builder()
                .ruleId("RULE007")
                .name("Expired Rule")
                .minAmount(new BigDecimal("10000"))
                .cashbackAmount(new BigDecimal("1000"))
                .cashbackType(CashbackType.FIXED)
                .validUntil(Instant.now().minusSeconds(86400)) // Expired yesterday
                .build();

        Transaction txn = createTransaction(new BigDecimal("50000"));

        boolean matches = rule.matches(txn);

        assertFalse(matches, "Expired rule should not match");
    }

    @Test
    @DisplayName("should not match inactive rule")
    void shouldNotMatchInactiveRule() {
        CashbackRule rule = CashbackRule.builder()
                .ruleId("RULE008")
                .name("Inactive Rule")
                .minAmount(new BigDecimal("10000"))
                .cashbackAmount(new BigDecimal("1000"))
                .cashbackType(CashbackType.FIXED)
                .active(false)
                .build();

        Transaction txn = createTransaction(new BigDecimal("50000"));

        boolean matches = rule.matches(txn);

        assertFalse(matches, "Inactive rule should not match");
    }

    @Test
    @DisplayName("should match when no merchant/category restrictions")
    void shouldMatchWithoutRestrictions() {
        CashbackRule rule = CashbackRule.builder()
                .ruleId("RULE009")
                .name("Universal Rule")
                .minAmount(new BigDecimal("10000"))
                .cashbackAmount(new BigDecimal("1000"))
                .cashbackType(CashbackType.FIXED)
                .build();

        Transaction txn = createTransaction(new BigDecimal("50000"), "ANY_MERCHANT", "ANY_CATEGORY");

        boolean matches = rule.matches(txn);

        assertTrue(matches, "Should match without restrictions");
    }

    @Test
    @DisplayName("should calculate cashback with proper rounding")
    void shouldCalculateWithRounding() {
        // 3.33% of 10000 = 333
        CashbackRule rule = CashbackRule.builder()
                .ruleId("RULE010")
                .name("Odd Percentage")
                .cashbackPercentage(new BigDecimal("3.33"))
                .cashbackType(CashbackType.PERCENTAGE)
                .build();

        Transaction txn = createTransaction(new BigDecimal("10000"));

        BigDecimal cashback = rule.calculateCashback(txn);

        assertEquals(new BigDecimal("333.00"), cashback, "Should round to 2 decimal places");
    }

    @Test
    @DisplayName("should preserve decimal percentage without floating-point conversion")
    void shouldPreserveDecimalPercentage() {
        BigDecimal percentage = new BigDecimal("3.35");

        CashbackRule rule = CashbackRule.builder()
                .ruleId("RULE-PRECISION")
                .cashbackType(CashbackType.PERCENTAGE)
                .cashbackPercentage(percentage)
                .build();

        assertEquals(percentage, rule.getCashbackPercentage());
        assertEquals(new BigDecimal("0.34"),
                rule.calculateCashback(createTransaction(new BigDecimal("10.00"))));
    }

    @Test
    @DisplayName("should return zero cashback for non-matching transaction")
    void shouldReturnZeroForNonMatching() {
        CashbackRule rule = CashbackRule.builder()
                .ruleId("RULE011")
                .name("High Minimum")
                .minAmount(new BigDecimal("100000"))
                .cashbackAmount(new BigDecimal("5000"))
                .cashbackType(CashbackType.FIXED)
                .build();

        Transaction txn = createTransaction(new BigDecimal("50000"));

        BigDecimal cashback = rule.calculateCashback(txn);

        assertEquals(BigDecimal.ZERO, cashback, "Should return zero for non-matching");
    }

    @Test
    @DisplayName("should match by exact amount for equals condition")
    void shouldMatchExactAmount() {
        CashbackRule rule = CashbackRule.builder()
                .ruleId("RULE012")
                .name("Exact Amount")
                .exactAmount(new BigDecimal("50000"))
                .cashbackAmount(new BigDecimal("5000"))
                .cashbackType(CashbackType.FIXED)
                .build();

        Transaction matchingTxn = createTransaction(new BigDecimal("50000"));
        Transaction nonMatchingTxn = createTransaction(new BigDecimal("50001"));

        assertTrue(rule.matches(matchingTxn), "Should match exact amount");
        assertFalse(rule.matches(nonMatchingTxn), "Should not match different amount");
    }

    @Test
    @DisplayName("should support tiered cashback based on amount ranges")
    void shouldSupportTieredCashback() {
        // Tiered rule: 50k-100k = 1000, 100k-200k = 3000, 200k+ = 5000
        CashbackRule rule = CashbackRule.builder()
                .ruleId("RULE013")
                .name("Tiered CashbackEntity")
                .minAmount(new BigDecimal("50000"))
                .tieredCashback(java.util.Map.of(
                        new BigDecimal("50000"), new BigDecimal("1000"),
                        new BigDecimal("100000"), new BigDecimal("3000"),
                        new BigDecimal("200000"), new BigDecimal("5000")
                ))
                .cashbackType(CashbackType.TIERED)
                .build();

        Transaction txn50k = createTransaction(new BigDecimal("75000"));
        Transaction txn150k = createTransaction(new BigDecimal("150000"));
        Transaction txn250k = createTransaction(new BigDecimal("250000"));

        assertEquals(new BigDecimal("1000"), rule.calculateCashback(txn50k), "50k tier");
        assertEquals(new BigDecimal("3000"), rule.calculateCashback(txn150k), "100k tier");
        assertEquals(new BigDecimal("5000"), rule.calculateCashback(txn250k), "200k tier");
    }
}
