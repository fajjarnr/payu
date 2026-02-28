package id.payu.dispute.application.service;

import id.payu.dispute.domain.model.Refund;
import id.payu.dispute.domain.model.RefundStatus;
import id.payu.dispute.domain.port.out.RefundPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RefundService.
 *
 * <p>These tests verify the application service logic for refund operations
 * using mocked persistence layer.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefundService Tests")
class RefundServiceTest {

    @Mock
    private RefundPersistencePort refundPersistencePort;

    private RefundService refundService;

    private static final UUID TRANSACTION_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID REFUND_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final BigDecimal AMOUNT = new BigDecimal("100000.00");
    private static final String CURRENCY = "IDR";
    private static final String REASON = "Customer request";

    @BeforeEach
    void setUp() {
        refundService = new RefundService(refundPersistencePort);
    }

    @Nested
    @DisplayName("Create Refund")
    class CreateRefundTests {

        @Test
        @DisplayName("Should create partial refund successfully")
        void shouldCreatePartialRefundSuccessfully() {
            // Given
            Refund expectedRefund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            when(refundPersistencePort.save(any(Refund.class))).thenReturn(expectedRefund);

            // When
            Refund result = refundService.createPartialRefund(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTransactionId()).isEqualTo(TRANSACTION_ID);
            assertThat(result.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(result.getCurrency()).isEqualTo(CURRENCY);
            assertThat(result.getReason()).isEqualTo(REASON);
            assertThat(result.getStatus()).isEqualTo(RefundStatus.PENDING);
            verify(refundPersistencePort).save(any(Refund.class));
        }
    }

    @Nested
    @DisplayName("Process Refund")
    class ProcessRefundTests {

        @Test
        @DisplayName("Should process pending refund successfully")
        void shouldProcessPendingRefundSuccessfully() {
            // Given
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            refund.setId(REFUND_ID);
            when(refundPersistencePort.findById(REFUND_ID)).thenReturn(Optional.of(refund));
            when(refundPersistencePort.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Refund result = refundService.processRefund(REFUND_ID);

            // Then
            assertThat(result.getStatus()).isEqualTo(RefundStatus.PROCESSING);
            assertThat(result.getProcessedAt()).isNotNull();
            verify(refundPersistencePort).findById(REFUND_ID);
            verify(refundPersistencePort).save(any(Refund.class));
        }

        @Test
        @DisplayName("Should throw exception when refund not found")
        void shouldThrowExceptionWhenRefundNotFound() {
            // Given
            when(refundPersistencePort.findById(REFUND_ID)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> refundService.processRefund(REFUND_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Refund not found");
        }
    }

    @Nested
    @DisplayName("Complete Refund")
    class CompleteRefundTests {

        @Test
        @DisplayName("Should complete processing refund successfully")
        void shouldCompleteProcessingRefundSuccessfully() {
            // Given
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            refund.setId(REFUND_ID);
            refund.process();
            when(refundPersistencePort.findById(REFUND_ID)).thenReturn(Optional.of(refund));
            when(refundPersistencePort.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Refund result = refundService.completeRefund(REFUND_ID);

            // Then
            assertThat(result.getStatus()).isEqualTo(RefundStatus.COMPLETED);
            assertThat(result.getCompletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Fail Refund")
    class FailRefundTests {

        @Test
        @DisplayName("Should fail processing refund successfully")
        void shouldFailProcessingRefundSuccessfully() {
            // Given
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            refund.setId(REFUND_ID);
            refund.process();
            when(refundPersistencePort.findById(REFUND_ID)).thenReturn(Optional.of(refund));
            when(refundPersistencePort.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Refund result = refundService.failRefund(REFUND_ID, "Insufficient funds");

            // Then
            assertThat(result.getStatus()).isEqualTo(RefundStatus.FAILED);
            assertThat(result.getFailureReason()).isEqualTo("Insufficient funds");
            assertThat(result.getFailedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Cancel Refund")
    class CancelRefundTests {

        @Test
        @DisplayName("Should cancel pending refund successfully")
        void shouldCancelPendingRefundSuccessfully() {
            // Given
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            refund.setId(REFUND_ID);
            when(refundPersistencePort.findById(REFUND_ID)).thenReturn(Optional.of(refund));
            when(refundPersistencePort.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Refund result = refundService.cancelRefund(REFUND_ID, "Customer changed mind");

            // Then
            assertThat(result.getStatus()).isEqualTo(RefundStatus.CANCELLED);
            assertThat(result.getCancelledAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Get Refund")
    class GetRefundTests {

        @Test
        @DisplayName("Should return refund when found")
        void shouldReturnRefundWhenFound() {
            // Given
            Refund refund = Refund.create(TRANSACTION_ID, AMOUNT, CURRENCY, REASON);
            refund.setId(REFUND_ID);
            when(refundPersistencePort.findById(REFUND_ID)).thenReturn(Optional.of(refund));

            // When
            Optional<Refund> result = refundService.getRefund(REFUND_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(REFUND_ID);
        }

        @Test
        @DisplayName("Should return empty when refund not found")
        void shouldReturnEmptyWhenRefundNotFound() {
            // Given
            when(refundPersistencePort.findById(REFUND_ID)).thenReturn(Optional.empty());

            // When
            Optional<Refund> result = refundService.getRefund(REFUND_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
