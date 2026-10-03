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

    Dispute openDispute(UUID transactionId, UUID customerId, UUID merchantId,
                        BigDecimal disputedAmount, String currency, String reason);

    Dispute startInvestigation(UUID disputeId, String investigationId);

    Dispute resolveDispute(UUID disputeId, DisputeResolutionType resolutionType, String resolution);

    Dispute rejectDispute(UUID disputeId, String rejectionReason);

    Dispute escalateDispute(UUID disputeId, String escalationReason);

    Dispute addEvidence(UUID disputeId, String fileName, String fileUrl, String uploadedBy);

    Optional<Dispute> getDispute(UUID disputeId);

    Optional<Dispute> getDisputeForCustomer(UUID disputeId, UUID customerId);

    List<Dispute> getDisputesByTransaction(UUID transactionId);

    List<Dispute> getDisputesByTransactionForCustomer(UUID transactionId, UUID customerId);

    List<Dispute> getDisputesByCustomer(UUID customerId);

    List<Dispute> getDisputesByMerchant(UUID merchantId);

    List<Dispute> getDisputesByStatus(String status);

    List<Dispute> getAllDisputes();
}
