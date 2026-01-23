package id.payu.transaction.adapter.persistence;

import id.payu.transaction.adapter.persistence.repository.ScheduledTransferJpaRepository;
import id.payu.transaction.domain.model.ScheduledTransfer;
import id.payu.transaction.domain.port.out.ScheduledTransferPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ScheduledTransferPersistenceAdapter implements ScheduledTransferPersistencePort {

    private final ScheduledTransferJpaRepository repository;

    @Override
    public ScheduledTransfer save(ScheduledTransfer scheduledTransfer) {
        scheduledTransfer.setUpdatedAt(Instant.now());
        return repository.save(scheduledTransfer);
    }

    @Override
    public Optional<ScheduledTransfer> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<ScheduledTransfer> findByReferenceNumber(String referenceNumber) {
        return repository.findByReferenceNumber(referenceNumber);
    }

    @Override
    public List<ScheduledTransfer> findBySenderAccountId(UUID senderAccountId) {
        return repository.findBySenderAccountId(senderAccountId);
    }

    @Override
    public List<ScheduledTransfer> findDueScheduledTransfers(Instant now) {
        return repository.findDueScheduledTransfers(now);
    }

    @Override
    public void delete(ScheduledTransfer scheduledTransfer) {
        repository.delete(scheduledTransfer);
    }
}
