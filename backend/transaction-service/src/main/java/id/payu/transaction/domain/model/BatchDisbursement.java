package id.payu.transaction.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate Root representing a batch of disbursements (bulk payout).
 *
 * <p>A batch disbursement groups multiple individual disbursements for efficient
 * bulk processing. This is commonly used for payroll, supplier payments, or
 * mass payouts. The batch tracks aggregate status and progress across all items.</p>
 *
 * <p>State Machine:</p>
 * <pre>
 * PENDING → PROCESSING → [COMPLETED | PARTIAL | FAILED]
 * </pre>
 *
 * <p>Key Business Rules:</p>
 * <ul>
 *   <li>Batch contains multiple Disbursement items</li>
 *   <li>Items are processed sequentially with error handling</li>
 *   <li>Continue on partial failure - don't stop batch for individual failures</li>
 *   <li>Aggregate status calculated from individual item statuses</li>
 *   <li>Progress tracking shows completion percentage</li>
 * </ul>
 *
 * <p>PCI-DSS Compliance:</p>
 * <ul>
 *   <li>Total amount calculated from individual Money values</li>
 *   <li>Audit trail via createdAt, startedAt, completedAt timestamps</li>
 *   <li>Idempotency key prevents duplicate batch creation</li>
 * </ul>
 *
 * @see Disbursement
 * @see BatchDisbursementStatus
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "batch_disbursements", indexes = {
    @Index(name = "idx_batch_source_account", columnList = "source_account_id"),
    @Index(name = "idx_batch_status", columnList = "status"),
    @Index(name = "idx_batch_created_at", columnList = "created_at"),
    @Index(name = "idx_batch_idempotency", columnList = "idempotency_key", unique = true)
})
public class BatchDisbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BatchDisbursementStatus status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    @Builder.Default
    private List<Disbursement> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Creates a new batch disbursement with the specified parameters.
     * Generates a unique idempotency key automatically.
     *
     * @param sourceAccountId the source wallet/account ID
     * @param name the batch name/description
     * @return a new BatchDisbursement in PENDING status
     * @throws IllegalArgumentException if any required parameter is invalid
     */
    public static BatchDisbursement create(UUID sourceAccountId, String name) {
        return createWithIdempotencyKey(sourceAccountId, name, generateIdempotencyKey());
    }

    /**
     * Creates a new batch disbursement with a specific idempotency key.
     *
     * @param sourceAccountId the source wallet/account ID
     * @param name the batch name/description
     * @param idempotencyKey the idempotency key for duplicate protection
     * @return a new BatchDisbursement in PENDING status
     * @throws IllegalArgumentException if any required parameter is invalid
     */
    public static BatchDisbursement createWithIdempotencyKey(UUID sourceAccountId, String name,
                                                              String idempotencyKey) {
        validateSourceAccountId(sourceAccountId);
        validateName(name);
        validateIdempotencyKey(idempotencyKey);

        BatchDisbursement batch = new BatchDisbursement();
        batch.id = UUID.randomUUID();
        batch.idempotencyKey = idempotencyKey;
        batch.sourceAccountId = sourceAccountId;
        batch.name = name;
        batch.status = BatchDisbursementStatus.PENDING;
        batch.items = new ArrayList<>();
        batch.createdAt = Instant.now();

        return batch;
    }

    /**
     * Adds a disbursement item to this batch.
     * Can only be called when batch is in PENDING status.
     *
     * @param item the disbursement item to add
     * @throws IllegalStateException if batch is not in PENDING status
     */
    public void addItem(Disbursement item) {
        if (status != BatchDisbursementStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot add items to batch in status: " + status + ". Expected: PENDING"
            );
        }
        this.items.add(item);
    }

    /**
     * Gets the list of disbursement items.
     * Returns an unmodifiable view to preserve encapsulation.
     *
     * @return unmodifiable list of items
     */
    public List<Disbursement> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Gets the total amount of all items in the batch.
     *
     * @return the total amount as Money
     */
    public Money getTotalAmount() {
        if (items.isEmpty()) {
            return Money.idr(BigDecimal.ZERO);
        }

        // Get currency from first item (all items should have same currency)
        String currency = items.get(0).getAmount().getCurrency().getCurrencyCode();
        Money total = Money.of(BigDecimal.ZERO, currency);

        for (Disbursement item : items) {
            total = total.add(item.getAmount());
        }

        return total;
    }

    /**
     * Gets the number of items in the batch.
     *
     * @return the item count
     */
    public int getItemCount() {
        return items.size();
    }

    /**
     * Calculates the aggregate status based on individual item statuses.
     *
     * @return the calculated aggregate status
     */
    public BatchDisbursementStatus calculateAggregateStatus() {
        if (items.isEmpty()) {
            return BatchDisbursementStatus.PENDING;
        }

        int completedCount = 0;
        int failedCount = 0;
        int processingCount = 0;

        for (Disbursement item : items) {
            switch (item.getStatus()) {
                case COMPLETED -> completedCount++;
                case FAILED -> failedCount++;
                case PROCESSING -> processingCount++;
                default -> {
                    // PENDING - no counting needed
                }
            }
        }

        int totalItems = items.size();

        if (completedCount == totalItems) {
            return BatchDisbursementStatus.COMPLETED;
        }
        if (failedCount == totalItems) {
            return BatchDisbursementStatus.FAILED;
        }
        if (completedCount > 0 || failedCount > 0 || processingCount > 0) {
            return BatchDisbursementStatus.PARTIAL;
        }

        return BatchDisbursementStatus.PENDING;
    }

    /**
     * Gets the count of items that have reached a terminal state (COMPLETED or FAILED).
     *
     * @return the number of processed items
     */
    public int getProcessedCount() {
        int count = 0;
        for (Disbursement item : items) {
            if (item.isTerminal()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the progress percentage (0-100) based on terminal items.
     *
     * @return the progress percentage
     */
    public int getProgressPercentage() {
        if (items.isEmpty()) {
            return 0;
        }
        return (getProcessedCount() * 100) / items.size();
    }

    /**
     * Transitions the batch from PENDING to PROCESSING.
     *
     * @throws IllegalStateException if batch is not in PENDING status
     */
    public void process() {
        if (status != BatchDisbursementStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot process batch in status: " + status + ". Expected: PENDING"
            );
        }
        this.status = BatchDisbursementStatus.PROCESSING;
        this.startedAt = Instant.now();
    }

    /**
     * Completes the batch and sets the final status based on aggregate calculation.
     *
     * @throws IllegalStateException if batch is not in PROCESSING status
     */
    public void complete() {
        if (status != BatchDisbursementStatus.PROCESSING) {
            throw new IllegalStateException(
                "Cannot complete batch in status: " + status + ". Expected: PROCESSING"
            );
        }
        this.status = calculateAggregateStatus();
        this.completedAt = Instant.now();
    }

    /**
     * Checks if this batch matches the given idempotency key.
     *
     * @param key the idempotency key to check
     * @return true if the keys match, false otherwise
     */
    public boolean matchesIdempotencyKey(String key) {
        return key != null && key.equals(this.idempotencyKey);
    }

    // ==================== VALIDATION METHODS ====================

    private static void validateSourceAccountId(UUID sourceAccountId) {
        if (sourceAccountId == null) {
            throw new IllegalArgumentException("Source account ID cannot be null");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
    }

    private static void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency key cannot be null or empty");
        }
    }

    private static String generateIdempotencyKey() {
        return "batch-" + UUID.randomUUID().toString().replace("-", "");
    }
}
