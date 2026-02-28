package id.payu.wallet.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SettlementBatchTest {

    @Test
    void shouldCreateSettlementBatch() {
        SettlementBatch batch = SettlementBatch.create("partner-123", LocalDate.now(), "IDR");

        assertNotNull(batch.getId());
        assertEquals("partner-123", batch.getPartnerId());
        assertEquals(LocalDate.now(), batch.getSettlementDate());
        assertEquals("IDR", batch.getCurrency());
        assertEquals(BigDecimal.ZERO, batch.getTotalAmount());
        assertEquals(SettlementBatch.SettlementStatus.PENDING, batch.getStatus());
        assertNotNull(batch.getCreatedAt());
    }

    @Test
    void shouldAddEntryAndRecalculateTotals() {
        SettlementBatch batch = SettlementBatch.create("partner-123", LocalDate.now(), "IDR");

        SettlementEntry entry = SettlementEntry.create(
                batch.getId(), "tx-1", "PAYMENT", "ref-1",
                new BigDecimal("100000.00"), "IDR", new BigDecimal("2500.00")
        );

        batch.addEntry(entry);

        assertEquals(1, batch.getEntries().size());
        assertEquals(new BigDecimal("100000.00"), batch.getTotalAmount());
        assertEquals(new BigDecimal("97500.00"), batch.getNetAmount());
    }

    @Test
    void shouldTransitionFromPendingToProcessing() {
        SettlementBatch batch = SettlementBatch.create("partner-123", LocalDate.now(), "IDR");

        batch.startProcessing("admin");

        assertEquals(SettlementBatch.SettlementStatus.PROCESSING, batch.getStatus());
        assertEquals("admin", batch.getProcessedBy());
        assertNotNull(batch.getProcessedAt());
    }

    @Test
    void shouldCompleteSettlement() {
        SettlementBatch batch = SettlementBatch.create("partner-123", LocalDate.now(), "IDR");
        batch.startProcessing("admin");

        batch.complete();

        assertEquals(SettlementBatch.SettlementStatus.COMPLETED, batch.getStatus());
    }

    @Test
    void shouldFailSettlement() {
        SettlementBatch batch = SettlementBatch.create("partner-123", LocalDate.now(), "IDR");
        batch.startProcessing("admin");

        batch.fail("Discrepancy detected");

        assertEquals(SettlementBatch.SettlementStatus.FAILED, batch.getStatus());
        assertEquals("Discrepancy detected", batch.getFailureReason());
    }

    @Test
    void shouldManualOverrideFailedSettlement() {
        SettlementBatch batch = SettlementBatch.create("partner-123", LocalDate.now(), "IDR");
        batch.startProcessing("admin");
        batch.fail("Discrepancy detected");

        batch.manualOverride("Approved by management", "superadmin");

        assertEquals(SettlementBatch.SettlementStatus.COMPLETED, batch.getStatus());
        assertTrue(batch.getFailureReason().contains("OVERRIDE"));
        assertTrue(batch.getFailureReason().contains("superadmin"));
    }

    @Test
    void shouldNotStartProcessingFromNonPendingStatus() {
        SettlementBatch batch = SettlementBatch.create("partner-123", LocalDate.now(), "IDR");
        batch.startProcessing("admin");
        batch.complete();

        assertThrows(IllegalStateException.class, () -> batch.startProcessing("admin2"));
    }

    @Test
    void shouldNotCompleteFromNonProcessingStatus() {
        SettlementBatch batch = SettlementBatch.create("partner-123", LocalDate.now(), "IDR");

        assertThrows(IllegalStateException.class, () -> batch.complete());
    }

    @Test
    void shouldNotOverrideNonFailedSettlement() {
        SettlementBatch batch = SettlementBatch.create("partner-123", LocalDate.now(), "IDR");

        assertThrows(IllegalStateException.class, () ->
                batch.manualOverride("reason", "admin"));
    }

    @Test
    void shouldAddDiscrepancy() {
        SettlementBatch batch = SettlementBatch.create("partner-123", LocalDate.now(), "IDR");

        Discrepancy discrepancy = Discrepancy.create(
                batch.getId(), "tx-1", Discrepancy.DiscrepancyType.AMOUNT_MISMATCH,
                "Amount mismatch detected",
                new BigDecimal("100000.00"), new BigDecimal("99000.00")
        );

        batch.addDiscrepancy(discrepancy);

        assertTrue(batch.hasDiscrepancies());
        assertEquals(1, batch.getDiscrepancies().size());
        assertEquals(new BigDecimal("-1000.00"), batch.getDiscrepancies().get(0).getDifference());
    }
}
