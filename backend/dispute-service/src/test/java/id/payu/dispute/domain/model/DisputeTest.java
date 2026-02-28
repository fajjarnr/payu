package id.payu.dispute.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Dispute Aggregate Root.
 *
 * <p>P0 Critical Tests - These tests verify the core dispute lifecycle
 * that must be correct for partner and customer protection.</p>
 *
 * <p>Test Categories:</p>
 * <ul>
 *   <li>Creation - Factory methods and initial state</li>
 *   <li>State Transitions - open, investigate, resolve, reject</li>
 *   <li>Evidence Management - addEvidence, getEvidenceList</li>
 *   <li>Invalid Transitions - Illegal state changes</li>
 *   <li>Resolution Types - refund, reject, partial</li>
 * </ul>
 *
 * @see Dispute
 */
@Execution(ExecutionMode.CONCURRENT)
@DisplayName("Dispute Aggregate Root Tests")
class DisputeTest {

    private static final UUID TRANSACTION_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID CUSTOMER_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
    private static final UUID MERCHANT_ID = UUID.fromString("770e8400-e29b-41d4-a716-446655440002");
    private static final BigDecimal DISPUTED_AMOUNT = new BigDecimal("100000.00");
    private static final String CURRENCY = "IDR";
    private static final String REASON = "Product not received";

    // ==================== CREATION TESTS ====================

    @Nested
    @DisplayName("Creation")
    class CreationTests {

        @Test
        @DisplayName("Should create dispute with open status")
        void shouldCreateDisputeWithOpenStatus() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);

            assertThat(dispute.getStatus()).isEqualTo(DisputeStatus.OPEN);
            assertThat(dispute.getTransactionId()).isEqualTo(TRANSACTION_ID);
            assertThat(dispute.getCustomerId()).isEqualTo(CUSTOMER_ID);
            assertThat(dispute.getMerchantId()).isEqualTo(MERCHANT_ID);
            assertThat(dispute.getDisputedAmount()).isEqualByComparingTo(DISPUTED_AMOUNT);
            assertThat(dispute.getCurrency()).isEqualTo(CURRENCY);
            assertThat(dispute.getReason()).isEqualTo(REASON);
            assertThat(dispute.getId()).isNotNull();
            assertThat(dispute.getOpenedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when transactionId is null")
        void shouldThrowExceptionWhenTransactionIdIsNull() {
            assertThatThrownBy(() -> Dispute.create(null, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Transaction ID cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when customerId is null")
        void shouldThrowExceptionWhenCustomerIdIsNull() {
            assertThatThrownBy(() -> Dispute.create(TRANSACTION_ID, null, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Customer ID cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when merchantId is null")
        void shouldThrowExceptionWhenMerchantIdIsNull() {
            assertThatThrownBy(() -> Dispute.create(TRANSACTION_ID, CUSTOMER_ID, null, DISPUTED_AMOUNT, CURRENCY, REASON))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Merchant ID cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when amount is null or invalid")
        void shouldThrowExceptionWhenAmountIsNullOrInvalid() {
            assertThatThrownBy(() -> Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, null, CURRENCY, REASON))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Disputed amount cannot be null");

            assertThatThrownBy(() -> Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, BigDecimal.ZERO, CURRENCY, REASON))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Disputed amount must be positive");
        }

        @Test
        @DisplayName("Should throw exception when currency is null or empty")
        void shouldThrowExceptionWhenCurrencyIsNullOrEmpty() {
            assertThatThrownBy(() -> Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, null, REASON))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currency cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception when reason is null or empty")
        void shouldThrowExceptionWhenReasonIsNullOrEmpty() {
            assertThatThrownBy(() -> Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Reason cannot be null or empty");
        }
    }

    // ==================== STATE TRANSITION TESTS ====================

    @Nested
    @DisplayName("State Transitions")
    class StateTransitionTests {

        @Test
        @DisplayName("Should start investigation from open status")
        void shouldStartInvestigation() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);

            dispute.startInvestigation("INV-001");

            assertThat(dispute.getStatus()).isEqualTo(DisputeStatus.INVESTIGATING);
            assertThat(dispute.getInvestigationStartedAt()).isNotNull();
            assertThat(dispute.getInvestigationId()).isEqualTo("INV-001");
        }

        @Test
        @DisplayName("Should resolve dispute in customer's favor with full refund")
        void shouldResolveDisputeInCustomerFavorWithFullRefund() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.startInvestigation("INV-001");

            dispute.resolve(DisputeResolutionType.REFUND_CUSTOMER, "Evidence supports customer claim");

            assertThat(dispute.getStatus()).isEqualTo(DisputeStatus.RESOLVED);
            assertThat(dispute.getResolutionType()).isEqualTo(DisputeResolutionType.REFUND_CUSTOMER);
            assertThat(dispute.getResolution()).isEqualTo("Evidence supports customer claim");
            assertThat(dispute.getResolvedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should resolve dispute in merchant's favor")
        void shouldResolveDisputeInMerchantFavor() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.startInvestigation("INV-001");

            dispute.resolve(DisputeResolutionType.REJECT_CLAIM, "Evidence supports merchant");

            assertThat(dispute.getStatus()).isEqualTo(DisputeStatus.RESOLVED);
            assertThat(dispute.getResolutionType()).isEqualTo(DisputeResolutionType.REJECT_CLAIM);
        }

        @Test
        @DisplayName("Should resolve with partial refund")
        void shouldResolveWithPartialRefund() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.startInvestigation("INV-001");

            dispute.resolve(DisputeResolutionType.PARTIAL_REFUND, "Partial liability accepted");

            assertThat(dispute.getStatus()).isEqualTo(DisputeStatus.RESOLVED);
            assertThat(dispute.getResolutionType()).isEqualTo(DisputeResolutionType.PARTIAL_REFUND);
        }

        @Test
        @DisplayName("Should reject dispute from open status")
        void shouldRejectDisputeFromOpenStatus() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);

            dispute.reject("Dispute filed after deadline");

            assertThat(dispute.getStatus()).isEqualTo(DisputeStatus.REJECTED);
            assertThat(dispute.getRejectionReason()).isEqualTo("Dispute filed after deadline");
            assertThat(dispute.getRejectedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should reject dispute from investigating status")
        void shouldRejectDisputeFromInvestigatingStatus() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.startInvestigation("INV-001");

            dispute.reject("Insufficient evidence");

            assertThat(dispute.getStatus()).isEqualTo(DisputeStatus.REJECTED);
        }

        @Test
        @DisplayName("Should escalate dispute")
        void shouldEscalateDispute() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.startInvestigation("INV-001");

            dispute.escalate("Requires senior review");

            assertThat(dispute.getStatus()).isEqualTo(DisputeStatus.ESCALATED);
            assertThat(dispute.getEscalationReason()).isEqualTo("Requires senior review");
            assertThat(dispute.getEscalatedAt()).isNotNull();
        }
    }

    // ==================== EVIDENCE MANAGEMENT TESTS ====================

    @Nested
    @DisplayName("Evidence Management")
    class EvidenceManagementTests {

        @Test
        @DisplayName("Should add evidence to dispute")
        void shouldAddEvidence() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);

            dispute.addEvidence("receipt.pdf", "https://storage.payu.fajjjar.my.id/evidence/receipt.pdf", "CUSTOMER");

            assertThat(dispute.getEvidenceList()).hasSize(1);
            DisputeEvidence evidence = dispute.getEvidenceList().get(0);
            assertThat(evidence.getFileName()).isEqualTo("receipt.pdf");
            assertThat(evidence.getFileUrl()).isEqualTo("https://storage.payu.fajjjar.my.id/evidence/receipt.pdf");
            assertThat(evidence.getUploadedBy()).isEqualTo("CUSTOMER");
            assertThat(evidence.getUploadedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should add multiple evidence items")
        void shouldAddMultipleEvidenceItems() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);

            dispute.addEvidence("receipt.pdf", "https://storage.payu.fajjjar.my.id/evidence/receipt.pdf", "CUSTOMER");
            dispute.addEvidence("photo.jpg", "https://storage.payu.fajjjar.my.id/evidence/photo.jpg", "CUSTOMER");
            dispute.addEvidence("response.pdf", "https://storage.payu.fajjjar.my.id/evidence/response.pdf", "MERCHANT");

            assertThat(dispute.getEvidenceList()).hasSize(3);
        }

        @Test
        @DisplayName("Should throw exception when adding evidence with invalid parameters")
        void shouldThrowExceptionWhenAddingEvidenceWithInvalidParameters() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);

            assertThatThrownBy(() -> dispute.addEvidence(null, "url", "CUSTOMER"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("File name cannot be null or empty");

            assertThatThrownBy(() -> dispute.addEvidence("file.pdf", null, "CUSTOMER"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("File URL cannot be null or empty");

            assertThatThrownBy(() -> dispute.addEvidence("file.pdf", "url", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Uploaded by cannot be null or empty");
        }

        @Test
        @DisplayName("Should not add evidence to resolved dispute")
        void shouldNotAddEvidenceToResolvedDispute() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.startInvestigation("INV-001");
            dispute.resolve(DisputeResolutionType.REFUND_CUSTOMER, "Resolved");

            assertThatThrownBy(() -> dispute.addEvidence("file.pdf", "url", "CUSTOMER"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot add evidence to dispute in status");
        }

        @Test
        @DisplayName("Should not add evidence to rejected dispute")
        void shouldNotAddEvidenceToRejectedDispute() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.reject("Rejected");

            assertThatThrownBy(() -> dispute.addEvidence("file.pdf", "url", "CUSTOMER"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot add evidence to dispute in status");
        }
    }

    // ==================== INVALID TRANSITION TESTS ====================

    @Nested
    @DisplayName("Invalid State Transitions")
    class InvalidTransitionTests {

        @Test
        @DisplayName("Should not resolve without investigation")
        void shouldNotResolveWithoutInvestigation() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);

            assertThatThrownBy(() -> dispute.resolve(DisputeResolutionType.REFUND_CUSTOMER, "Resolved"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot resolve dispute in status");
        }

        @Test
        @DisplayName("Should not start investigation on already investigating dispute")
        void shouldNotStartInvestigationOnAlreadyInvestigatingDispute() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.startInvestigation("INV-001");

            assertThatThrownBy(() -> dispute.startInvestigation("INV-002"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot start investigation for dispute in status");
        }

        @Test
        @DisplayName("Should not start investigation on resolved dispute")
        void shouldNotStartInvestigationOnResolvedDispute() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.startInvestigation("INV-001");
            dispute.resolve(DisputeResolutionType.REFUND_CUSTOMER, "Resolved");

            assertThatThrownBy(() -> dispute.startInvestigation("INV-002"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot start investigation for dispute in status");
        }

        @Test
        @DisplayName("Should not resolve already resolved dispute")
        void shouldNotResolveAlreadyResolvedDispute() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.startInvestigation("INV-001");
            dispute.resolve(DisputeResolutionType.REFUND_CUSTOMER, "Resolved");

            assertThatThrownBy(() -> dispute.resolve(DisputeResolutionType.REJECT_CLAIM, "Again"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot resolve dispute in status");
        }

        @Test
        @DisplayName("Should not reject already rejected dispute")
        void shouldNotRejectAlreadyRejectedDispute() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.reject("First rejection");

            assertThatThrownBy(() -> dispute.reject("Second rejection"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot reject dispute in status");
        }

        @Test
        @DisplayName("Should not reject resolved dispute")
        void shouldNotRejectResolvedDispute() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.startInvestigation("INV-001");
            dispute.resolve(DisputeResolutionType.REFUND_CUSTOMER, "Resolved");

            assertThatThrownBy(() -> dispute.reject("Try reject"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot reject dispute in status");
        }

        @Test
        @DisplayName("Should not escalate without investigation")
        void shouldNotEscalateWithoutInvestigation() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);

            assertThatThrownBy(() -> dispute.escalate("Escalate"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot escalate dispute in status");
        }

        @Test
        @DisplayName("Should not escalate already escalated dispute")
        void shouldNotEscalateAlreadyEscalatedDispute() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.startInvestigation("INV-001");
            dispute.escalate("First escalation");

            assertThatThrownBy(() -> dispute.escalate("Second escalation"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot escalate dispute in status");
        }
    }

    // ==================== BUSINESS RULE TESTS ====================

    @Nested
    @DisplayName("Business Rules")
    class BusinessRuleTests {

        @Test
        @DisplayName("Should be in terminal state when resolved")
        void shouldBeInTerminalStateWhenResolved() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.startInvestigation("INV-001");
            dispute.resolve(DisputeResolutionType.REFUND_CUSTOMER, "Resolved");

            assertThat(dispute.isInTerminalState()).isTrue();
        }

        @Test
        @DisplayName("Should be in terminal state when rejected")
        void shouldBeInTerminalStateWhenRejected() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.reject("Rejected");

            assertThat(dispute.isInTerminalState()).isTrue();
        }

        @Test
        @DisplayName("Should not be in terminal state when open")
        void shouldNotBeInTerminalStateWhenOpen() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);

            assertThat(dispute.isInTerminalState()).isFalse();
        }

        @Test
        @DisplayName("Should not be in terminal state when investigating")
        void shouldNotBeInTerminalStateWhenInvestigating() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.startInvestigation("INV-001");

            assertThat(dispute.isInTerminalState()).isFalse();
        }

        @Test
        @DisplayName("Should require resolution type when resolving")
        void shouldRequireResolutionTypeWhenResolving() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.startInvestigation("INV-001");

            assertThatThrownBy(() -> dispute.resolve(null, "Resolved"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Resolution type cannot be null");
        }

        @Test
        @DisplayName("Should require resolution description when resolving")
        void shouldRequireResolutionDescriptionWhenResolving() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.startInvestigation("INV-001");

            assertThatThrownBy(() -> dispute.resolve(DisputeResolutionType.REFUND_CUSTOMER, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Resolution cannot be null or empty");

            assertThatThrownBy(() -> dispute.resolve(DisputeResolutionType.REFUND_CUSTOMER, ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Resolution cannot be null or empty");
        }

        @Test
        @DisplayName("Should require rejection reason when rejecting")
        void shouldRequireRejectionReasonWhenRejecting() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);

            assertThatThrownBy(() -> dispute.reject(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Rejection reason cannot be null or empty");

            assertThatThrownBy(() -> dispute.reject(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Rejection reason cannot be null or empty");
        }

        @Test
        @DisplayName("Should require investigation ID when starting investigation")
        void shouldRequireInvestigationIdWhenStartingInvestigation() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);

            assertThatThrownBy(() -> dispute.startInvestigation(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Investigation ID cannot be null or empty");

            assertThatThrownBy(() -> dispute.startInvestigation(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Investigation ID cannot be null or empty");
        }
    }

    // ==================== EQUALITY TESTS ====================

    @Nested
    @DisplayName("Equality and Identity")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when same ID")
        void shouldBeEqualWhenSameId() {
            UUID id = UUID.randomUUID();
            Dispute dispute1 = Dispute.builder()
                    .id(id)
                    .transactionId(TRANSACTION_ID)
                    .customerId(CUSTOMER_ID)
                    .merchantId(MERCHANT_ID)
                    .disputedAmount(DISPUTED_AMOUNT)
                    .currency(CURRENCY)
                    .reason(REASON)
                    .status(DisputeStatus.OPEN)
                    .openedAt(Instant.now())
                    .evidenceList(List.of())
                    .build();

            Dispute dispute2 = Dispute.builder()
                    .id(id)
                    .transactionId(TRANSACTION_ID)
                    .customerId(CUSTOMER_ID)
                    .merchantId(MERCHANT_ID)
                    .disputedAmount(DISPUTED_AMOUNT)
                    .currency(CURRENCY)
                    .reason(REASON)
                    .status(DisputeStatus.OPEN)
                    .openedAt(Instant.now())
                    .evidenceList(List.of())
                    .build();

            assertThat(dispute1).isEqualTo(dispute2);
        }

        @Test
        @DisplayName("Should not be equal when different ID")
        void shouldNotBeEqualWhenDifferentId() {
            Dispute dispute1 = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            Dispute dispute2 = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);

            assertThat(dispute1).isNotEqualTo(dispute2);
        }
    }
}
