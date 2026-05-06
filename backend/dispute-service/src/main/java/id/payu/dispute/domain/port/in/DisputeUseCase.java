package id.payu.dispute.domain.port.in;

import id.payu.dispute.domain.model.Dispute;
import id.payu.dispute.domain.model.DisputeResolutionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for dispute use cases.
 *
 * <p>Defines the operations that can be performed on disputes.
 * This interface is implemented by application services.</p>
 */
public interface DisputeUseCase {

    /**
     * Opens a new dispute.
     *
     * @param transactionId  the disputed transaction
     * @param customerId     the customer opening the dispute
     * @param merchantId     the merchant involved
     * @param disputedAmount the amount being disputed
     * @param currency       the currency code
     * @param reason         the reason for dispute
     * @return the created dispute
     */
    Dispute openDispute(UUID transactionId, UUID customerId, UUID merchantId,
                        BigDecimal disputedAmount, String currency, String reason);

    /**
     * Starts investigation on a dispute.
     *
     * @param disputeId       the dispute ID
     * @param investigationId the investigation identifier
     * @return the updated dispute
     */
    Dispute startInvestigation(UUID disputeId, String investigationId);

    /**
     * Resolves a dispute.
     *
     * @param disputeId      the dispute ID
     * @param resolutionType the type of resolution
     * @param resolution     the resolution description
     * @return the updated dispute
     */
    Dispute resolveDispute(UUID disputeId, DisputeResolutionType resolutionType, String resolution);

    /**
     * Rejects a dispute.
     *
     * @param disputeId       the dispute ID
     * @param rejectionReason the reason for rejection
     * @return the updated dispute
     */
    Dispute rejectDispute(UUID disputeId, String rejectionReason);

    /**
     * Escalates a dispute.
     *
     * @param disputeId        the dispute ID
     * @param escalationReason the reason for escalation
     * @return the updated dispute
     */
    Dispute escalateDispute(UUID disputeId, String escalationReason);

    /**
     * Adds evidence to a dispute.
     *
     * @param disputeId  the dispute ID
     * @param fileName   the evidence file name
     * @param fileUrl    the evidence file URL
     * @param uploadedBy who uploaded the evidence
     * @return the updated dispute
     */
    Dispute addEvidence(UUID disputeId, String fileName, String fileUrl, String uploadedBy);

    /**
     * Gets a dispute by ID.
     *
     * @param disputeId the dispute ID
     * @return optional containing the dispute if found
     */
    Optional<Dispute> getDispute(UUID disputeId);

    /**
     * Gets all disputes for a transaction.
     *
     * @param transactionId the transaction ID
     * @return list of disputes
     */
    List<Dispute> getDisputesByTransaction(UUID transactionId);

    /**
     * Gets disputes by customer.
     *
     * @param customerId the customer ID
     * @return list of disputes
     */
    List<Dispute> getDisputesByCustomer(UUID customerId);

    /**
     * Gets disputes by merchant.
     *
     * @param merchantId the merchant ID
     * @return list of disputes
     */
    List<Dispute> getDisputesByMerchant(UUID merchantId);

    /**
     * Gets disputes by status.
     *
     * @param status the dispute status
     * @return list of disputes
     */
    List<Dispute> getDisputesByStatus(String status);

    /**
     * Gets all disputes.
     *
     * @return list of all disputes
     */
    List<Dispute> getAllDisputes();
}
