package id.payu.partner.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Domain tests for Refund aggregate root.
 * TDD: Red phase - tests first, implementation second.
 */
@DisplayName("Refund Domain Model Tests")
class RefundTest {

    @Nested
    @DisplayName("Refund Creation")
    class RefundCreationTests {

        @Test
        @DisplayName("should create refund with pending status")
        void shouldCreateRefundWithPendingStatus() {
            UUID transactionId = UUID.randomUUID();
            UUID partnerId = UUID.randomUUID();
            BigDecimal amount = new BigDecimal("100000.00");
            String reason = "Customer request";
            String requestedBy = "customer@example.com";

            Refund refund = Refund.create(transactionId, partnerId, amount, reason, requestedBy);

            assertNotNull(refund.getId());
            assertEquals(transactionId, refund.getTransactionId());
            assertEquals(partnerId, refund.getPartnerId());
            assertEquals(amount, refund.getAmount());
            assertEquals(reason, refund.getReason());
            assertEquals(requestedBy, refund.getRequestedBy());
            assertEquals(RefundStatus.PENDING, refund.getStatus());
            assertNotNull(refund.getRequestedAt());
            assertNull(refund.getProcessedAt());
            assertNull(refund.getCompletedAt());
            assertNull(refund.getFailureReason());
        }

        @Test
        @DisplayName("should not create refund with null transaction id")
        void shouldNotCreateRefundWithNullTransactionId() {
            assertThrows(IllegalArgumentException.class, () ->
                Refund.create(null, UUID.randomUUID(), BigDecimal.valueOf(100000), "reason", "user")
            );
        }

        @Test
        @DisplayName("should not create refund with null amount")
        void shouldNotCreateRefundWithNullAmount() {
            assertThrows(IllegalArgumentException.class, () ->
                Refund.create(UUID.randomUUID(), UUID.randomUUID(), null, "reason", "user")
            );
        }

        @Test
        @DisplayName("should not create refund with zero or negative amount")
        void shouldNotCreateRefundWithZeroOrNegativeAmount() {
            UUID transactionId = UUID.randomUUID();
            UUID partnerId = UUID.randomUUID();

            assertThrows(IllegalArgumentException.class, () ->
                Refund.create(transactionId, partnerId, BigDecimal.ZERO, "reason", "user")
            );

            assertThrows(IllegalArgumentException.class, () ->
                Refund.create(transactionId, partnerId, new BigDecimal("-100"), "reason", "user")
            );
        }

        @Test
        @DisplayName("should not create refund with blank reason")
        void shouldNotCreateRefundWithBlankReason() {
            UUID transactionId = UUID.randomUUID();
            UUID partnerId = UUID.randomUUID();

            assertThrows(IllegalArgumentException.class, () ->
                Refund.create(transactionId, partnerId, BigDecimal.valueOf(100000), "", "user")
            );

            assertThrows(IllegalArgumentException.class, () ->
                Refund.create(transactionId, partnerId, BigDecimal.valueOf(100000), "   ", "user")
            );
        }
    }

    @Nested
    @DisplayName("Refund Processing")
    class RefundProcessingTests {

        @Test
        @DisplayName("should process refund from pending status")
        void shouldProcessRefund() {
            Refund refund = Refund.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100000.00"),
                "Customer request",
                "customer@example.com"
            );
            String processorId = "processor-123";

            refund.process(processorId);

            assertEquals(RefundStatus.PROCESSING, refund.getStatus());
            assertEquals(processorId, refund.getProcessorId());
            assertNotNull(refund.getProcessedAt());
        }

        @Test
        @DisplayName("should not process already processed refund")
        void shouldNotProcessAlreadyProcessedRefund() {
            Refund refund = Refund.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100000.00"),
                "Customer request",
                "customer@example.com"
            );
            refund.process("processor-123");

            assertThrows(IllegalStateException.class, () ->
                refund.process("processor-456")
            );
        }

        @Test
        @DisplayName("should not process completed refund")
        void shouldNotProcessCompletedRefund() {
            Refund refund = Refund.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100000.00"),
                "Customer request",
                "customer@example.com"
            );
            refund.process("processor-123");
            refund.complete("refund-txn-456");

            assertThrows(IllegalStateException.class, () ->
                refund.process("processor-789")
            );
        }

        @Test
        @DisplayName("should not process failed refund")
        void shouldNotProcessFailedRefund() {
            Refund refund = Refund.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100000.00"),
                "Customer request",
                "customer@example.com"
            );
            refund.process("processor-123");
            refund.fail("Insufficient funds");

            assertThrows(IllegalStateException.class, () ->
                refund.process("processor-789")
            );
        }
    }

    @Nested
    @DisplayName("Refund Completion")
    class RefundCompletionTests {

        @Test
        @DisplayName("should complete refund from processing status")
        void shouldCompleteRefund() {
            Refund refund = Refund.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100000.00"),
                "Customer request",
                "customer@example.com"
            );
            refund.process("processor-123");
            String refundTransactionId = "refund-txn-456";

            refund.complete(refundTransactionId);

            assertEquals(RefundStatus.COMPLETED, refund.getStatus());
            assertEquals(refundTransactionId, refund.getRefundTransactionId());
            assertNotNull(refund.getCompletedAt());
        }

        @Test
        @DisplayName("should not complete refund not in processing status")
        void shouldNotCompleteRefundNotInProcessingStatus() {
            Refund refund = Refund.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100000.00"),
                "Customer request",
                "customer@example.com"
            );

            assertThrows(IllegalStateException.class, () ->
                refund.complete("refund-txn-456")
            );
        }

        @Test
        @DisplayName("should not complete already completed refund")
        void shouldNotCompleteAlreadyCompletedRefund() {
            Refund refund = Refund.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100000.00"),
                "Customer request",
                "customer@example.com"
            );
            refund.process("processor-123");
            refund.complete("refund-txn-456");

            assertThrows(IllegalStateException.class, () ->
                refund.complete("refund-txn-789")
            );
        }
    }

    @Nested
    @DisplayName("Refund Failure")
    class RefundFailureTests {

        @Test
        @DisplayName("should fail refund from processing status")
        void shouldFailRefund() {
            Refund refund = Refund.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100000.00"),
                "Customer request",
                "customer@example.com"
            );
            refund.process("processor-123");
            String failureReason = "Insufficient funds in source account";

            refund.fail(failureReason);

            assertEquals(RefundStatus.FAILED, refund.getStatus());
            assertEquals(failureReason, refund.getFailureReason());
        }

        @Test
        @DisplayName("should not fail refund not in processing status")
        void shouldNotFailRefundNotInProcessingStatus() {
            Refund refund = Refund.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100000.00"),
                "Customer request",
                "customer@example.com"
            );

            assertThrows(IllegalStateException.class, () ->
                refund.fail("Some reason")
            );
        }

        @Test
        @DisplayName("should not fail with blank reason")
        void shouldNotFailWithBlankReason() {
            Refund refund = Refund.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100000.00"),
                "Customer request",
                "customer@example.com"
            );
            refund.process("processor-123");

            assertThrows(IllegalArgumentException.class, () ->
                refund.fail("")
            );

            assertThrows(IllegalArgumentException.class, () ->
                refund.fail("   ")
            );
        }
    }

    @Nested
    @DisplayName("Refund Status Checks")
    class RefundStatusChecksTests {

        @Test
        @DisplayName("should correctly identify terminal states")
        void shouldCorrectlyIdentifyTerminalStates() {
            Refund pendingRefund = Refund.create(
                UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(100000), "reason", "user"
            );
            assertFalse(pendingRefund.isTerminal());

            Refund processingRefund = Refund.create(
                UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(100000), "reason", "user"
            );
            processingRefund.process("processor-123");
            assertFalse(processingRefund.isTerminal());

            Refund completedRefund = Refund.create(
                UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(100000), "reason", "user"
            );
            completedRefund.process("processor-123");
            completedRefund.complete("txn-123");
            assertTrue(completedRefund.isTerminal());

            Refund failedRefund = Refund.create(
                UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(100000), "reason", "user"
            );
            failedRefund.process("processor-123");
            failedRefund.fail("reason");
            assertTrue(failedRefund.isTerminal());
        }
    }
}
