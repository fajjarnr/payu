package id.payu.transaction.domain.port.out;

import id.payu.transaction.domain.model.ScheduledTransfer;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduledTransferPersistencePort {

    ScheduledTransfer save(ScheduledTransfer scheduledTransfer);

    Optional<ScheduledTransfer> findById(UUID id);

    Optional<ScheduledTransfer> findByReferenceNumber(String referenceNumber);

    List<ScheduledTransfer> findBySenderAccountId(UUID senderAccountId);

    List<ScheduledTransfer> findDueScheduledTransfers(Instant now);

    void delete(ScheduledTransfer scheduledTransfer);
}
