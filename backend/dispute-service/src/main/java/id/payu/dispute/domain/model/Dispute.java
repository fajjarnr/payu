package id.payu.dispute.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate Root representing a dispute between customer and merchant.
 *
 * <p>This aggregate manages the lifecycle of a dispute from opening to resolution.
 * It supports evidence collection and enforces proper state transitions.</p>
 *
 * <p>State Machine:
 * <pre>
 * OPEN -> INVESTIGATING -> RESOLVED
 *                      -> ESCALATED
 *       -> REJECTED (from OPEN or INVESTIGATING)
 * </pre></p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dispute {

    private UUID id;
    private UUID transactionId;
    private UUID customerId;
    private UUID merchantId;
    private BigDecimal disputedAmount;
    private String currency;
    private String reason;
    private DisputeStatus status;
    private String investigationId;
    private DisputeResolutionType resolutionType;
    private String resolution;
    private String rejectionReason;
    private String escalationReason;
    private Instant openedAt;
    private Instant investigationStartedAt;
    private Instant resolvedAt;
    private Instant rejectedAt;
    private Instant escalatedAt;

    @Builder.Default
    private List<DisputeEvidence> evidenceList = new ArrayList<>();

    /**
     * Creates a new dispute.
     *
     * @param transactionId  the disputed transaction
     * @param customerId     the customer who opened the dispute
     * @param merchantId     the merchant involved
     * @param disputedAmount the amount being disputed
     * @param currency       the currency code
     * @param reason         the reason for dispute
     * @return a new Dispute instance in OPEN status
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public static Dispute create(UUID transactionId, UUID customerId, UUID merchantId,
                                 BigDecimal disputedAmount, String currency, String reason) {
        validateCreationParameters(transactionId, customerId, merchantId, disputedAmount, currency, reason);

        return Dispute.builder()
                .id(UUID.randomUUID())
                .transactionId(transactionId)
                .customerId(customerId)
                .merchantId(merchantId)
                .disputedAmount(disputedAmount)
                .currency(currency)
                .reason(reason)
                .status(DisputeStatus.OPEN)
                .openedAt(Instant.now())
                .evidenceList(new ArrayList<>())
                .build();
    }

    /**
     * Starts investigation, transitioning from OPEN to INVESTIGATING.
     *
     * @param investigationId the investigation identifier
     * @throws IllegalStateException if the dispute is not in OPEN status
     * @throws IllegalArgumentException if investigationId is null or empty
     */
    public void startInvestigation(String investigationId) {
        if (status != DisputeStatus.OPEN) {
            throw new IllegalStateException("Cannot start investigation for dispute in status: " + status);
        }
        if (investigationId == null || investigationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Investigation ID cannot be null or empty");
        }
        this.status = DisputeStatus.INVESTIGATING;
        this.investigationId = investigationId;
        this.investigationStartedAt = Instant.now();
    }

    /**
     * Resolves the dispute, transitioning from INVESTIGATING to RESOLVED.
     *
     * @param resolutionType the type of resolution
     * @param resolution     the resolution description
     * @throws IllegalStateException if the dispute is not in INVESTIGATING status
     * @throws IllegalArgumentException if resolution parameters are invalid
     */
    public void resolve(DisputeResolutionType resolutionType, String resolution) {
        if (status != DisputeStatus.INVESTIGATING) {
            throw new IllegalStateException("Cannot resolve dispute in status: " + status);
        }
        if (resolutionType == null) {
            throw new IllegalArgumentException("Resolution type cannot be null");
        }
        if (resolution == null || resolution.trim().isEmpty()) {
            throw new IllegalArgumentException("Resolution cannot be null or empty");
        }
        this.status = DisputeStatus.RESOLVED;
        this.resolutionType = resolutionType;
        this.resolution = resolution;
        this.resolvedAt = Instant.now();
    }

    /**
     * Rejects the dispute, transitioning to REJECTED.
     *
     * @param rejectionReason the reason for rejection
     * @throws IllegalStateException if the dispute is already resolved or rejected
     * @throws IllegalArgumentException if rejectionReason is null or empty
     */
    public void reject(String rejectionReason) {
        if (status == DisputeStatus.RESOLVED || status == DisputeStatus.REJECTED) {
            throw new IllegalStateException("Cannot reject dispute in status: " + status);
        }
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason cannot be null or empty");
        }
        this.status = DisputeStatus.REJECTED;
        this.rejectionReason = rejectionReason;
        this.rejectedAt = Instant.now();
    }

    /**
     * Escalates the dispute, transitioning from INVESTIGATING to ESCALATED.
     *
     * @param escalationReason the reason for escalation
     * @throws IllegalStateException if the dispute is not in INVESTIGATING status
     * @throws IllegalArgumentException if escalationReason is null or empty
     */
    public void escalate(String escalationReason) {
        if (status != DisputeStatus.INVESTIGATING) {
            throw new IllegalStateException("Cannot escalate dispute in status: " + status);
        }
        if (escalationReason == null || escalationReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Escalation reason cannot be null or empty");
        }
        this.status = DisputeStatus.ESCALATED;
        this.escalationReason = escalationReason;
        this.escalatedAt = Instant.now();
    }

    /**
     * Adds evidence to the dispute.
     *
     * @param fileName   the name of the evidence file
     * @param fileUrl    the URL where the file is stored
     * @param uploadedBy who uploaded the evidence
     * @throws IllegalStateException if the dispute is in a terminal state
     * @throws IllegalArgumentException if evidence parameters are invalid
     */
    public void addEvidence(String fileName, String fileUrl, String uploadedBy) {
        if (isInTerminalState()) {
            throw new IllegalStateException("Cannot add evidence to dispute in status: " + status);
        }
        DisputeEvidence evidence = DisputeEvidence.create(fileName, fileUrl, uploadedBy);
        this.evidenceList.add(evidence);
    }

    /**
     * Checks if the dispute is in a terminal state.
     *
     * @return true if the dispute is RESOLVED or REJECTED
     */
    public boolean isInTerminalState() {
        return status == DisputeStatus.RESOLVED
                || status == DisputeStatus.REJECTED;
    }

    private static void validateCreationParameters(UUID transactionId, UUID customerId, UUID merchantId,
                                                   BigDecimal disputedAmount, String currency, String reason) {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        if (merchantId == null) {
            throw new IllegalArgumentException("Merchant ID cannot be null");
        }
        if (disputedAmount == null) {
            throw new IllegalArgumentException("Disputed amount cannot be null");
        }
        if (disputedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Disputed amount must be positive");
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be null or empty");
        }
    }
}
