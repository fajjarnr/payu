package id.payu.transaction.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for BatchDisbursement Aggregate Root.
 *
 * <p>P0 Critical Tests - These tests verify batch processing logic
 * and aggregate status calculation that must be correct for bulk payouts.</p>
 *
 * <p>Test Categories:</p>
 * <ul>
 *   <li>Factory Methods - create()</li>
 *   <li>Item Management - addItem(), getItems()</li>
 *   <li>Status Calculation - calculateAggregateStatus()</li>
 *   <li>Progress Tracking - getTotalAmount(), getProcessedCount()</li>
 *   <li>State Transitions - process(), complete()</li>
 *   <li>Edge Cases - empty batch, all failed, partial completion</li>
 * </ul>
 *
 * @see BatchDisbursement
 */
@Execution(ExecutionMode.CONCURRENT)
@DisplayName("BatchDisbursement Aggregate Root Tests")
class BatchDisbursementTest {

    private static final UUID SOURCE_ACCOUNT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String BATCH_NAME = "Monthly Salary Payments";
    private static final String IDEMPOTENCY_KEY = "batch-idem-key-123";

    // ==================== FACTORY METHOD TESTS ====================

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodsTests {

        @Test
        @DisplayName("Should create batch disbursement with PENDING status")
        void shouldCreateBatchDisbursementWithPendingStatus() {
            BatchDisbursement batch = BatchDisbursement.create(
                    SOURCE_ACCOUNT_ID,
                    BATCH_NAME
            );

            assertThat(batch.getStatus()).isEqualTo(BatchDisbursementStatus.PENDING);
            assertThat(batch.getSourceAccountId()).isEqualTo(SOURCE_ACCOUNT_ID);
            assertThat(batch.getName()).isEqualTo(BATCH_NAME);
            assertThat(batch.getItems()).isEmpty();
            assertThat(batch.getCreatedAt()).isNotNull();
            assertThat(batch.getId()).isNotNull();
        }

        @Test
        @DisplayName("Should create batch disbursement with idempotency key")
        void shouldCreateBatchDisbursementWithIdempotencyKey() {
            BatchDisbursement batch = BatchDisbursement.createWithIdempotencyKey(
                    SOURCE_ACCOUNT_ID,
                    BATCH_NAME,
                    IDEMPOTENCY_KEY
            );

            assertThat(batch.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
            assertThat(batch.getStatus()).isEqualTo(BatchDisbursementStatus.PENDING);
        }

        @Test
        @DisplayName("Should generate idempotency key if not provided")
        void shouldGenerateIdempotencyKeyIfNotProvided() {
            BatchDisbursement batch = BatchDisbursement.create(
                    SOURCE_ACCOUNT_ID,
                    BATCH_NAME
            );

            assertThat(batch.getIdempotencyKey()).isNotNull();
            assertThat(batch.getIdempotencyKey()).isNotEmpty();
        }

        @Test
        @DisplayName("Should throw exception when source account ID is null")
        void shouldThrowExceptionWhenSourceAccountIdIsNull() {
            assertThatThrownBy(() -> BatchDisbursement.create(
                    null,
                    BATCH_NAME
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Source account ID cannot be null");
        }

        @Test
        @DisplayName("Should throw exception when name is null or empty")
        void shouldThrowExceptionWhenNameIsNullOrEmpty() {
            assertThatThrownBy(() -> BatchDisbursement.create(
                    SOURCE_ACCOUNT_ID,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Name cannot be null or empty");

            assertThatThrownBy(() -> BatchDisbursement.create(
                    SOURCE_ACCOUNT_ID,
                    ""
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Name cannot be null or empty");
        }
    }

    // ==================== ITEM MANAGEMENT TESTS ====================

    @Nested
    @DisplayName("Item Management")
    class ItemManagementTests {

        @Test
        @DisplayName("Should add item to batch")
        void shouldAddItemToBatch() {
            BatchDisbursement batch = createSampleBatch();
            Disbursement item = createSampleDisbursement();

            batch.addItem(item);

            assertThat(batch.getItems()).hasSize(1);
            assertThat(batch.getItems().get(0)).isEqualTo(item);
        }

        @Test
        @DisplayName("Should calculate total amount correctly")
        void shouldCalculateTotalAmountCorrectly() {
            BatchDisbursement batch = createSampleBatch();
            batch.addItem(createDisbursement(Money.idr("100000")));
            batch.addItem(createDisbursement(Money.idr("200000")));
            batch.addItem(createDisbursement(Money.idr("300000")));

            Money total = batch.getTotalAmount();

            assertThat(total).isEqualTo(Money.idr("600000"));
        }

        @Test
        @DisplayName("Should return zero for total amount when no items")
        void shouldReturnZeroForTotalAmountWhenNoItems() {
            BatchDisbursement batch = createSampleBatch();

            Money total = batch.getTotalAmount();

            assertThat(total).isEqualTo(Money.idr("0"));
        }

        @Test
        @DisplayName("Should return correct item count")
        void shouldReturnCorrectItemCount() {
            BatchDisbursement batch = createSampleBatch();
            batch.addItem(createSampleDisbursement());
            batch.addItem(createSampleDisbursement());

            assertThat(batch.getItemCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return immutable items list")
        void shouldReturnImmutableItemsList() {
            BatchDisbursement batch = createSampleBatch();
            List<Disbursement> items = batch.getItems();

            assertThatThrownBy(() -> items.add(createSampleDisbursement()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ==================== STATUS CALCULATION TESTS ====================

    @Nested
    @DisplayName("Status Calculation")
    class StatusCalculationTests {

        @Test
        @DisplayName("Should return PENDING when all items are PENDING")
        void shouldReturnPendingWhenAllItemsArePending() {
            BatchDisbursement batch = createSampleBatch();
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.PENDING));
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.PENDING));

            assertThat(batch.calculateAggregateStatus()).isEqualTo(BatchDisbursementStatus.PENDING);
        }

        @Test
        @DisplayName("Should return PARTIAL when some items are PROCESSING")
        void shouldReturnPartialWhenSomeItemsAreProcessing() {
            BatchDisbursement batch = createSampleBatch();
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.PENDING));
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.PROCESSING));

            assertThat(batch.calculateAggregateStatus()).isEqualTo(BatchDisbursementStatus.PARTIAL);
        }

        @Test
        @DisplayName("Should return COMPLETED when all items are COMPLETED")
        void shouldReturnCompletedWhenAllItemsAreCompleted() {
            BatchDisbursement batch = createSampleBatch();
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.COMPLETED));
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.COMPLETED));

            assertThat(batch.calculateAggregateStatus()).isEqualTo(BatchDisbursementStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should return FAILED when all items are FAILED")
        void shouldReturnFailedWhenAllItemsAreFailed() {
            BatchDisbursement batch = createSampleBatch();
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.FAILED));
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.FAILED));

            assertThat(batch.calculateAggregateStatus()).isEqualTo(BatchDisbursementStatus.FAILED);
        }

        @Test
        @DisplayName("Should return PARTIAL when mix of COMPLETED and FAILED")
        void shouldReturnPartialWhenMixOfCompletedAndFailed() {
            BatchDisbursement batch = createSampleBatch();
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.COMPLETED));
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.FAILED));

            assertThat(batch.calculateAggregateStatus()).isEqualTo(BatchDisbursementStatus.PARTIAL);
        }

        @Test
        @DisplayName("Should return PARTIAL when mix of all statuses")
        void shouldReturnPartialWhenMixOfAllStatuses() {
            BatchDisbursement batch = createSampleBatch();
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.COMPLETED));
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.FAILED));
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.PROCESSING));
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.PENDING));

            assertThat(batch.calculateAggregateStatus()).isEqualTo(BatchDisbursementStatus.PARTIAL);
        }
    }

    // ==================== PROGRESS TRACKING TESTS ====================

    @Nested
    @DisplayName("Progress Tracking")
    class ProgressTrackingTests {

        @Test
        @DisplayName("Should return correct processed count")
        void shouldReturnCorrectProcessedCount() {
            BatchDisbursement batch = createSampleBatch();

            Disbursement pending = createDisbursementWithStatus(DisbursementStatus.PENDING);
            Disbursement processing = createDisbursementWithStatus(DisbursementStatus.PROCESSING);
            Disbursement completed = createDisbursementWithStatus(DisbursementStatus.COMPLETED);
            Disbursement failed = createDisbursementWithStatus(DisbursementStatus.FAILED);

            batch.addItem(pending);
            batch.addItem(processing);
            batch.addItem(completed);
            batch.addItem(failed);

            assertThat(batch.getProcessedCount()).isEqualTo(2); // completed + failed
        }

        @Test
        @DisplayName("Should return correct progress percentage")
        void shouldReturnCorrectProgressPercentage() {
            BatchDisbursement batch = createSampleBatch();
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.COMPLETED));
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.FAILED));
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.PENDING));
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.PROCESSING));

            int progress = batch.getProgressPercentage();

            assertThat(progress).isEqualTo(50); // 2 out of 4 are terminal
        }

        @Test
        @DisplayName("Should return zero progress for empty batch")
        void shouldReturnZeroProgressForEmptyBatch() {
            BatchDisbursement batch = createSampleBatch();

            assertThat(batch.getProgressPercentage()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return zero progress when no items processed")
        void shouldReturnZeroProgressWhenNoItemsProcessed() {
            BatchDisbursement batch = createSampleBatch();
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.PENDING));
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.PROCESSING));

            assertThat(batch.getProgressPercentage()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return 100 progress when all items processed")
        void shouldReturn100ProgressWhenAllItemsProcessed() {
            BatchDisbursement batch = createSampleBatch();
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.COMPLETED));
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.FAILED));

            assertThat(batch.getProgressPercentage()).isEqualTo(100);
        }
    }

    // ==================== STATE TRANSITION TESTS ====================

    @Nested
    @DisplayName("State Transitions")
    class StateTransitionTests {

        @Test
        @DisplayName("Should transition from PENDING to PROCESSING")
        void shouldTransitionFromPendingToProcessing() {
            BatchDisbursement batch = createSampleBatch();

            batch.process();

            assertThat(batch.getStatus()).isEqualTo(BatchDisbursementStatus.PROCESSING);
            assertThat(batch.getStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when processing non-PENDING batch")
        void shouldThrowExceptionWhenProcessingNonPendingBatch() {
            BatchDisbursement batch = createSampleBatch();
            batch.process();

            assertThatThrownBy(batch::process)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot process batch in status");
        }

        @Test
        @DisplayName("Should complete batch and set aggregate status")
        void shouldCompleteBatchAndSetAggregateStatus() {
            BatchDisbursement batch = createSampleBatch();
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.COMPLETED));
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.COMPLETED));
            batch.process();

            batch.complete();

            assertThat(batch.getStatus()).isEqualTo(BatchDisbursementStatus.COMPLETED);
            assertThat(batch.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should complete batch with PARTIAL status")
        void shouldCompleteBatchWithPartialStatus() {
            BatchDisbursement batch = createSampleBatch();
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.COMPLETED));
            batch.addItem(createDisbursementWithStatus(DisbursementStatus.FAILED));
            batch.process();

            batch.complete();

            assertThat(batch.getStatus()).isEqualTo(BatchDisbursementStatus.PARTIAL);
        }

        @Test
        @DisplayName("Should throw exception when completing non-PROCESSING batch")
        void shouldThrowExceptionWhenCompletingNonProcessingBatch() {
            BatchDisbursement batch = createSampleBatch();

            assertThatThrownBy(batch::complete)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot complete batch in status");
        }
    }

    // ==================== HELPER METHODS ====================

    private BatchDisbursement createSampleBatch() {
        return BatchDisbursement.create(SOURCE_ACCOUNT_ID, BATCH_NAME);
    }

    private Disbursement createSampleDisbursement() {
        return Disbursement.create(
                SOURCE_ACCOUNT_ID,
                Money.idr(new BigDecimal("100000")),
                "014",
                "1234567890",
                "John Doe"
        );
    }

    private Disbursement createDisbursement(Money amount) {
        return Disbursement.create(
                SOURCE_ACCOUNT_ID,
                amount,
                "014",
                "1234567890",
                "John Doe"
        );
    }

    private Disbursement createDisbursementWithStatus(DisbursementStatus status) {
        Disbursement disbursement = Disbursement.create(
                SOURCE_ACCOUNT_ID,
                Money.idr(new BigDecimal("100000")),
                "014",
                "1234567890",
                "John Doe"
        );

        switch (status) {
            case PROCESSING:
                disbursement.process();
                break;
            case COMPLETED:
                disbursement.process();
                disbursement.complete("REF" + System.nanoTime());
                break;
            case FAILED:
                disbursement.process();
                disbursement.fail("Test failure");
                break;
            default:
                // PENDING - no action needed
                break;
        }

        return disbursement;
    }
}
