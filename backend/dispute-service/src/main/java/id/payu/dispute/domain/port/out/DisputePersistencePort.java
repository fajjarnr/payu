package id.payu.dispute.domain.port.out;

import id.payu.dispute.domain.model.Dispute;
import id.payu.dispute.domain.model.DisputeStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DisputePersistencePort {
    Dispute save(Dispute dispute);
    Optional<Dispute> findById(UUID id);
    List<Dispute> findByTransactionId(UUID transactionId);
    List<Dispute> findByCustomerId(UUID customerId);
    List<Dispute> findByMerchantId(UUID merchantId);
    List<Dispute> findByStatus(DisputeStatus status);
    List<Dispute> findAll();
    void deleteById(UUID id);
}
