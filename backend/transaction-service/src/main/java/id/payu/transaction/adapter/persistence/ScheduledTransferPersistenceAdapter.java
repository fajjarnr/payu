package id.payu.transaction.adapter.persistence;

import id.payu.transaction.adapter.persistence.repository.ScheduledTransferJpaRepository;
import id.payu.transaction.adapter.persistence.entity.ScheduledTransferEntity;
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
    public ScheduledTransferEntity save(ScheduledTransferEntity scheduledTransfer) {
        scheduledTransfer.setUpdatedAt(Instant.now());
        return repository.save(scheduledTransfer);
    }

    @Override
    public ScheduledTransferEntity persistNew(ScheduledTransferEntity scheduledTransfer) {
        if (scheduledTransfer.getId() == null) {
            throw new IllegalStateException("Cannot persistNew: id is null");
        }
        scheduledTransfer.setUpdatedAt(Instant.now());
        repository.persistNew(scheduledTransfer);
        return repository.findById(scheduledTransfer.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "ScheduledTransfer not found after persist: " + scheduledTransfer.getId()));
    }

    @Override
    public Optional<ScheduledTransferEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<ScheduledTransferEntity> findByReferenceNumber(String referenceNumber) {
        return repository.findByReferenceNumber(referenceNumber);
    }

    @Override
    public List<ScheduledTransferEntity> findBySenderAccountId(UUID senderAccountId) {
        return repository.findBySenderAccountId(senderAccountId);
    }

    @Override
    public List<ScheduledTransferEntity> findDueScheduledTransfers(Instant now) {
        return repository.findDueScheduledTransfers(now);
    }

    @Override
    public void delete(ScheduledTransferEntity scheduledTransfer) {
        repository.delete(scheduledTransfer);
    }
}
