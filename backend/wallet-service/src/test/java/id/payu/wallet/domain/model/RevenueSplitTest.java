package id.payu.wallet.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RevenueSplitTest {

    @Test
    void shouldCreateRevenueSplit() {
        RevenueSplit split = RevenueSplit.create("partner-123", "Revenue Share Q1", "Q1 2024 revenue split", RevenueSplit.SplitType.PERCENTAGE, "admin");

        assertNotNull(split.getId());
        assertEquals("partner-123", split.getPartnerId());
        assertEquals("Revenue Share Q1", split.getName());
        assertEquals(RevenueSplit.SplitType.PERCENTAGE, split.getSplitType());
        assertTrue(split.isActive());
        assertEquals("admin", split.getCreatedBy());
    }

    @Test
    void shouldAddStakeholder() {
        RevenueSplit split = RevenueSplit.create("partner-123", "Revenue Share", "Test", RevenueSplit.SplitType.PERCENTAGE, "admin");

        split.addStakeholder("acc-1", "Partner A", new BigDecimal("60"), null, 1);

        assertEquals(1, split.getStakeholders().size());
        assertEquals("acc-1", split.getStakeholders().get(0).getAccountId());
        assertEquals("Partner A", split.getStakeholders().get(0).getName());
        assertEquals(new BigDecimal("60"), split.getStakeholders().get(0).getPercentage());
    }

    @Test
    void shouldCalculatePercentageSplits() {
        RevenueSplit split = RevenueSplit.create("partner-123", "Revenue Share", "Test", RevenueSplit.SplitType.PERCENTAGE, "admin");
        split.addStakeholder("acc-1", "Partner A", new BigDecimal("60"), null, 1);
        split.addStakeholder("acc-2", "Partner B", new BigDecimal("40"), null, 1);

        List<CalculatedSplit> results = split.calculateSplits(new BigDecimal("100000.00"));

        assertEquals(2, results.size());
        assertEquals(new BigDecimal("60000.00"), results.get(0).getAmount());
        assertEquals(new BigDecimal("40000.00"), results.get(1).getAmount());
    }

    @Test
    void shouldCalculateFixedSplits() {
        RevenueSplit split = RevenueSplit.create("partner-123", "Revenue Share", "Test", RevenueSplit.SplitType.FIXED, "admin");
        split.addStakeholder("acc-1", "Partner A", null, new BigDecimal("50000.00"), 1);
        split.addStakeholder("acc-2", "Partner B", null, new BigDecimal("30000.00"), 2);

        List<CalculatedSplit> results = split.calculateSplits(new BigDecimal("100000.00"));

        assertEquals(2, results.size());
        assertEquals(new BigDecimal("50000.00"), results.get(0).getAmount());
        assertEquals(new BigDecimal("30000.00"), results.get(1).getAmount());
    }

    @Test
    void shouldCalculateMixedSplits() {
        RevenueSplit split = RevenueSplit.create("partner-123", "Revenue Share", "Test", RevenueSplit.SplitType.MIXED, "admin");
        split.addStakeholder("acc-1", "Partner A", new BigDecimal("50"), new BigDecimal("10000.00"), 1);
        split.addStakeholder("acc-2", "Partner B", new BigDecimal("30"), null, 2);

        List<CalculatedSplit> results = split.calculateSplits(new BigDecimal("100000.00"));

        assertEquals(2, results.size());
        // Partner A: 10000 + (100000 * 50%) = 60000
        assertEquals(new BigDecimal("60000.00"), results.get(0).getAmount());
        // Partner B: (100000 * 30%) = 30000
        assertEquals(new BigDecimal("30000.00"), results.get(1).getAmount());
    }

    @Test
    void shouldRespectPriorityOrder() {
        RevenueSplit split = RevenueSplit.create("partner-123", "Revenue Share", "Test", RevenueSplit.SplitType.FIXED, "admin");
        split.addStakeholder("acc-1", "Partner A", null, new BigDecimal("80000.00"), 2);
        split.addStakeholder("acc-2", "Partner B", null, new BigDecimal("50000.00"), 1);

        List<CalculatedSplit> results = split.calculateSplits(new BigDecimal("100000.00"));

        // Partner B has higher priority (lower number), so gets paid first
        assertEquals("Partner B", results.get(0).getName());
        assertEquals("Partner A", results.get(1).getName());
        // Partner B gets full 50000
        assertEquals(new BigDecimal("50000.00"), results.get(0).getAmount());
        // Partner A gets remaining 50000 (capped by remaining amount)
        assertEquals(new BigDecimal("50000.00"), results.get(1).getAmount());
    }

    @Test
    void shouldValidatePercentageSplit() {
        RevenueSplit split = RevenueSplit.create("partner-123", "Revenue Share", "Test", RevenueSplit.SplitType.PERCENTAGE, "admin");
        split.addStakeholder("acc-1", "Partner A", new BigDecimal("60"), null, 1);
        split.addStakeholder("acc-2", "Partner B", new BigDecimal("30"), null, 1);

        assertTrue(split.isValid());
    }

    @Test
    void shouldInvalidateExcessivePercentage() {
        RevenueSplit split = RevenueSplit.create("partner-123", "Revenue Share", "Test", RevenueSplit.SplitType.PERCENTAGE, "admin");
        split.addStakeholder("acc-1", "Partner A", new BigDecimal("70"), null, 1);
        split.addStakeholder("acc-2", "Partner B", new BigDecimal("40"), null, 1);

        // 70 + 40 = 110%, which exceeds 100%
        assertFalse(split.isValid());
    }

    @Test
    void shouldDeactivateRevenueSplit() {
        RevenueSplit split = RevenueSplit.create("partner-123", "Revenue Share", "Test", RevenueSplit.SplitType.PERCENTAGE, "admin");

        split.deactivate();

        assertFalse(split.isActive());
        assertNotNull(split.getEffectiveUntil());
    }

    @Test
    void shouldCheckEffectiveAt() {
        RevenueSplit split = RevenueSplit.create("partner-123", "Revenue Share", "Test", RevenueSplit.SplitType.PERCENTAGE, "admin");

        assertTrue(split.isEffectiveAt(java.time.LocalDateTime.now()));
    }
}
