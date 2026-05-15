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
    List<SplitBillEntity> findByCreatorAccountId(UUID accountId, Pageable pageable);
}
