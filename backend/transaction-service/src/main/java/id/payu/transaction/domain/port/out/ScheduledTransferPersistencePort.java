package id.payu.transaction.domain.port.out;

import id.payu.transaction.adapter.persistence.entity.ScheduledTransferEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduledTransferPersistencePort {

    ScheduledTransferEntity save(ScheduledTransferEntity scheduledTransfer);

    Optional<ScheduledTransferEntity> findById(UUID id);

    Optional<ScheduledTransferEntity> findByReferenceNumber(String referenceNumber);

    List<ScheduledTransferEntity> findBySenderAccountId(UUID senderAccountId);

    List<ScheduledTransferEntity> findDueScheduledTransfers(Instant now);

    void delete(ScheduledTransferEntity scheduledTransfer);
}
