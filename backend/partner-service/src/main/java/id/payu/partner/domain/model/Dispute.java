package id.payu.partner.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Domain model for partner dispute management.
 * Implements rich domain behavior for dispute lifecycle.
 */
public class Dispute {

    private UUID id;
    private UUID transactionId;
    private String partnerId;
    private String reason;
    private String openedBy;
    private DisputeStatus status;
    private Instant openedAt;
    private Instant resolvedAt;
    private String resolution;
    private List<String> evidenceUrls;
    private String investigatorId;
    private Instant investigationStartedAt;
    private String rejectionReason;

    private Dispute() {
        this.evidenceUrls = new ArrayList<>();
    }

    /**
     * Factory method to open a new dispute.
     */
    public static Dispute open(UUID transactionId, String partnerId, String reason, String openedBy) {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        if (partnerId == null || partnerId.isBlank()) {
            throw new IllegalArgumentException("Partner ID cannot be blank");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason cannot be blank");
        }
        if (openedBy == null || openedBy.isBlank()) {
            throw new IllegalArgumentException("OpenedBy cannot be blank");
        }
        Dispute dispute = new Dispute();
        dispute.id = UUID.randomUUID();
        dispute.transactionId = transactionId;
        dispute.partnerId = partnerId;
        dispute.reason = reason;
        dispute.openedBy = openedBy;
        dispute.status = DisputeStatus.OPEN;
        dispute.openedAt = Instant.now();
        return dispute;
    }

    /**
     * Start investigation on this dispute.
     */
    public void startInvestigation(String investigatorId) {
        if (this.status != DisputeStatus.OPEN) {
            throw new IllegalStateException("Cannot start investigation on dispute in " + status + " status");
        }
        if (investigatorId == null || investigatorId.isBlank()) {
            throw new IllegalArgumentException("Investigator ID cannot be blank");
        }
        this.investigatorId = investigatorId;
        this.status = DisputeStatus.UNDER_INVESTIGATION;
        this.investigationStartedAt = Instant.now();
    }

    /**
     * Resolve the dispute with a resolution message.
     */
    public void resolve(String resolution) {
        if (this.status != DisputeStatus.UNDER_INVESTIGATION) {
            throw new IllegalStateException("Cannot resolve dispute in " + status + " status");
        }
        if (resolution == null || resolution.isBlank()) {
            throw new IllegalArgumentException("Resolution cannot be blank");
        }
        this.resolution = resolution;
        this.status = DisputeStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }

    /**
     * Reject the dispute with a reason.
     */
    public void reject(String reason) {
        if (this.status != DisputeStatus.UNDER_INVESTIGATION) {
            throw new IllegalStateException("Cannot reject dispute in " + status + " status");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason cannot be blank");
        }
        this.rejectionReason = reason;
        this.status = DisputeStatus.REJECTED;
        this.resolvedAt = Instant.now();
    }

    /**
     * Add evidence URL to this dispute.
     */
    public void addEvidence(String evidenceUrl) {
        if (isClosed()) {
            throw new IllegalStateException("Cannot add evidence to closed dispute");
        }
        if (evidenceUrl == null || evidenceUrl.isBlank()) {
            throw new IllegalArgumentException("Evidence URL cannot be blank");
        }
        if (this.evidenceUrls.contains(evidenceUrl)) {
            throw new IllegalArgumentException("Evidence URL already exists");
        }
        this.evidenceUrls.add(evidenceUrl);
    }

    /**
     * Check if dispute is in OPEN status.
     */
    public boolean isOpen() {
        return status == DisputeStatus.OPEN;
    }

    /**
     * Check if dispute is under investigation.
     */
    public boolean isUnderInvestigation() {
        return status == DisputeStatus.UNDER_INVESTIGATION;
    }

    /**
     * Check if dispute is closed (RESOLVED or REJECTED).
     */
    public boolean isClosed() {
        return status == DisputeStatus.RESOLVED || status == DisputeStatus.REJECTED;
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public String getReason() {
        return reason;
    }

    public String getOpenedBy() {
        return openedBy;
    }

    public DisputeStatus getStatus() {
        return status;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public String getResolution() {
        return resolution;
    }

    public List<String> getEvidenceUrls() {
        return Collections.unmodifiableList(evidenceUrls);
    }

    public String getInvestigatorId() {
        return investigatorId;
    }

    public Instant getInvestigationStartedAt() {
        return investigationStartedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}
