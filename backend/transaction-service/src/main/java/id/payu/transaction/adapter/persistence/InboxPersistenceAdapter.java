package id.payu.transaction.adapter.persistence;

import id.payu.transaction.adapter.persistence.entity.InboxEventEntity;
import id.payu.transaction.adapter.persistence.repository.InboxEventJpaRepository;
import id.payu.transaction.domain.port.out.InboxPersistencePort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class InboxPersistenceAdapter implements InboxPersistencePort {
    private final InboxEventJpaRepository repo;

    public InboxPersistenceAdapter(InboxEventJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public boolean existsByReferenceNo(String referenceNo) {
        return repo.existsByReferenceNo(referenceNo);
    }

    @Override
    public Optional<String> findPayloadByReferenceNo(String referenceNo) {
        return repo.findByReferenceNo(referenceNo).map(InboxEventEntity::getPayload);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(String referenceNo, String payload) {
        try {
            InboxEventEntity e = new InboxEventEntity(UUID.randomUUID(), referenceNo, payload, Instant.now());
            repo.saveAndFlush(e);
        } catch (DataIntegrityViolationException ex) {
            // concurrent duplicate, ignore
        }
    }
}
