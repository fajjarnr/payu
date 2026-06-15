package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.adapter.persistence.entity.SplitBillEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SplitBillJpaRepository extends JpaRepository<SplitBillEntity, UUID> {
    Optional<SplitBillEntity> findByReferenceNumber(String referenceNumber);

    /**
     * READY-071: Fetch participants eagerly to avoid LazyInitializationException
     * during JSON serialization (JPA session is closed after the @Transactional
     * boundary). Per JPA best practice, use @EntityGraph for the eager fetch.
     */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"participants"})
    List<SplitBillEntity> findByCreatorAccountId(UUID accountId, Pageable pageable);
}
