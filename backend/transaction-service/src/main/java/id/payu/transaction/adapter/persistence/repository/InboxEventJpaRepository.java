package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.adapter.persistence.entity.InboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InboxEventJpaRepository extends JpaRepository<InboxEventEntity, UUID> {
    Optional<InboxEventEntity> findByReferenceNo(String referenceNo);
    boolean existsByReferenceNo(String referenceNo);
}
