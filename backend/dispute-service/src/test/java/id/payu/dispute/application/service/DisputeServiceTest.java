package id.payu.dispute.application.service;

import id.payu.dispute.domain.model.Dispute;
import id.payu.dispute.domain.model.DisputeResolutionType;
import id.payu.dispute.domain.model.DisputeStatus;
import id.payu.dispute.domain.port.out.DisputePersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DisputeService.
 *
 * <p>These tests verify the application service logic for dispute operations
 * using mocked persistence layer.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DisputeService Tests")
class DisputeServiceTest {

    @Mock
    private DisputePersistencePort disputePersistencePort;

    private DisputeService disputeService;

    private static final UUID TRANSACTION_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID CUSTOMER_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
    private static final UUID MERCHANT_ID = UUID.fromString("770e8400-e29b-41d4-a716-446655440002");
    private static final UUID DISPUTE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");
    private static final BigDecimal DISPUTED_AMOUNT = new BigDecimal("100000.00");
    private static final String CURRENCY = "IDR";
    private static final String REASON = "Product not received";

    @BeforeEach
    void setUp() {
        disputeService = new DisputeService(disputePersistencePort);
    }

    @Nested
    @DisplayName("Open Dispute")
    class OpenDisputeTests {

        @Test
        @DisplayName("Should open dispute successfully")
        void shouldOpenDisputeSuccessfully() {
            // Given
            Dispute expectedDispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            when(disputePersistencePort.save(any(Dispute.class))).thenReturn(expectedDispute);

            // When
            Dispute result = disputeService.openDispute(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTransactionId()).isEqualTo(TRANSACTION_ID);
            assertThat(result.getCustomerId()).isEqualTo(CUSTOMER_ID);
            assertThat(result.getMerchantId()).isEqualTo(MERCHANT_ID);
            assertThat(result.getDisputedAmount()).isEqualByComparingTo(DISPUTED_AMOUNT);
            assertThat(result.getCurrency()).isEqualTo(CURRENCY);
            assertThat(result.getReason()).isEqualTo(REASON);
            assertThat(result.getStatus()).isEqualTo(DisputeStatus.OPEN);
            verify(disputePersistencePort).save(any(Dispute.class));
        }
    }

    @Nested
    @DisplayName("Start Investigation")
    class StartInvestigationTests {

        @Test
        @DisplayName("Should start investigation successfully")
        void shouldStartInvestigationSuccessfully() {
            // Given
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.setId(DISPUTE_ID);
            when(disputePersistencePort.findById(DISPUTE_ID)).thenReturn(Optional.of(dispute));
            when(disputePersistencePort.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Dispute result = disputeService.startInvestigation(DISPUTE_ID, "INV-001");

            // Then
            assertThat(result.getStatus()).isEqualTo(DisputeStatus.INVESTIGATING);
            assertThat(result.getInvestigationId()).isEqualTo("INV-001");
            assertThat(result.getInvestigationStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when dispute not found")
        void shouldThrowExceptionWhenDisputeNotFound() {
            // Given
            when(disputePersistencePort.findById(DISPUTE_ID)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> disputeService.startInvestigation(DISPUTE_ID, "INV-001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Dispute not found");
        }
    }

    @Nested
    @DisplayName("Resolve Dispute")
    class ResolveDisputeTests {

        @Test
        @DisplayName("Should resolve dispute in customer's favor")
        void shouldResolveDisputeInCustomerFavor() {
            // Given
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.setId(DISPUTE_ID);
            dispute.startInvestigation("INV-001");
            when(disputePersistencePort.findById(DISPUTE_ID)).thenReturn(Optional.of(dispute));
            when(disputePersistencePort.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Dispute result = disputeService.resolveDispute(DISPUTE_ID, DisputeResolutionType.REFUND_CUSTOMER, "Evidence supports customer");

            // Then
            assertThat(result.getStatus()).isEqualTo(DisputeStatus.RESOLVED);
            assertThat(result.getResolutionType()).isEqualTo(DisputeResolutionType.REFUND_CUSTOMER);
            assertThat(result.getResolution()).isEqualTo("Evidence supports customer");
            assertThat(result.getResolvedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should resolve dispute with partial refund")
        void shouldResolveDisputeWithPartialRefund() {
            // Given
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.setId(DISPUTE_ID);
            dispute.startInvestigation("INV-001");
            when(disputePersistencePort.findById(DISPUTE_ID)).thenReturn(Optional.of(dispute));
            when(disputePersistencePort.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Dispute result = disputeService.resolveDispute(DISPUTE_ID, DisputeResolutionType.PARTIAL_REFUND, "Partial liability");

            // Then
            assertThat(result.getResolutionType()).isEqualTo(DisputeResolutionType.PARTIAL_REFUND);
        }
    }

    @Nested
    @DisplayName("Reject Dispute")
    class RejectDisputeTests {

        @Test
        @DisplayName("Should reject open dispute")
        void shouldRejectOpenDispute() {
            // Given
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.setId(DISPUTE_ID);
            when(disputePersistencePort.findById(DISPUTE_ID)).thenReturn(Optional.of(dispute));
            when(disputePersistencePort.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Dispute result = disputeService.rejectDispute(DISPUTE_ID, "Dispute filed after deadline");

            // Then
            assertThat(result.getStatus()).isEqualTo(DisputeStatus.REJECTED);
            assertThat(result.getRejectionReason()).isEqualTo("Dispute filed after deadline");
            assertThat(result.getRejectedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Escalate Dispute")
    class EscalateDisputeTests {

        @Test
        @DisplayName("Should escalate investigating dispute")
        void shouldEscalateInvestigatingDispute() {
            // Given
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.setId(DISPUTE_ID);
            dispute.startInvestigation("INV-001");
            when(disputePersistencePort.findById(DISPUTE_ID)).thenReturn(Optional.of(dispute));
            when(disputePersistencePort.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Dispute result = disputeService.escalateDispute(DISPUTE_ID, "Requires senior review");

            // Then
            assertThat(result.getStatus()).isEqualTo(DisputeStatus.ESCALATED);
            assertThat(result.getEscalationReason()).isEqualTo("Requires senior review");
            assertThat(result.getEscalatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Add Evidence")
    class AddEvidenceTests {

        @Test
        @DisplayName("Should add evidence to open dispute")
        void shouldAddEvidenceToOpenDispute() {
            // Given
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.setId(DISPUTE_ID);
            when(disputePersistencePort.findById(DISPUTE_ID)).thenReturn(Optional.of(dispute));
            when(disputePersistencePort.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Dispute result = disputeService.addEvidence(DISPUTE_ID, "receipt.pdf", "https://storage.payu.fajjjar.my.id/evidence/receipt.pdf", "CUSTOMER");

            // Then
            assertThat(result.getEvidenceList()).hasSize(1);
            assertThat(result.getEvidenceList().get(0).getFileName()).isEqualTo("receipt.pdf");
        }
    }

    @Nested
    @DisplayName("Get Dispute")
    class GetDisputeTests {

        @Test
        @DisplayName("Should return dispute when found")
        void shouldReturnDisputeWhenFound() {
            // Given
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID, DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.setId(DISPUTE_ID);
            when(disputePersistencePort.findById(DISPUTE_ID)).thenReturn(Optional.of(dispute));

            // When
            Optional<Dispute> result = disputeService.getDispute(DISPUTE_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(DISPUTE_ID);
        }

        @Test
        @DisplayName("Should return empty when dispute not found")
        void shouldReturnEmptyWhenDisputeNotFound() {
            // Given
            when(disputePersistencePort.findById(DISPUTE_ID)).thenReturn(Optional.empty());

            // When
            Optional<Dispute> result = disputeService.getDispute(DISPUTE_ID);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should scope dispute lookup to customer")
        void shouldScopeDisputeLookupToCustomer() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID,
                    DISPUTED_AMOUNT, CURRENCY, REASON);
            dispute.setId(DISPUTE_ID);
            when(disputePersistencePort.findByIdAndCustomerId(DISPUTE_ID, CUSTOMER_ID))
                    .thenReturn(Optional.of(dispute));

            Optional<Dispute> result = disputeService.getDisputeForCustomer(DISPUTE_ID, CUSTOMER_ID);

            assertThat(result).containsSame(dispute);
            verify(disputePersistencePort).findByIdAndCustomerId(DISPUTE_ID, CUSTOMER_ID);
        }

        @Test
        @DisplayName("Should scope transaction lookup to customer")
        void shouldScopeTransactionLookupToCustomer() {
            Dispute dispute = Dispute.create(TRANSACTION_ID, CUSTOMER_ID, MERCHANT_ID,
                    DISPUTED_AMOUNT, CURRENCY, REASON);
            when(disputePersistencePort.findByTransactionIdAndCustomerId(TRANSACTION_ID, CUSTOMER_ID))
                    .thenReturn(java.util.List.of(dispute));

            List<Dispute> result = disputeService.getDisputesByTransactionForCustomer(TRANSACTION_ID, CUSTOMER_ID);

            assertThat(result).containsExactly(dispute);
            verify(disputePersistencePort).findByTransactionIdAndCustomerId(TRANSACTION_ID, CUSTOMER_ID);
        }
    }
}
