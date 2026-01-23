package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.domain.model.SplitBill;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SplitBillJpaRepository extends JpaRepository<SplitBill, UUID> {
    Optional<SplitBill> findByReferenceNumber(String referenceNumber);
    List<SplitBill> findByCreatorAccountId(UUID accountId, Pageable pageable);
}
