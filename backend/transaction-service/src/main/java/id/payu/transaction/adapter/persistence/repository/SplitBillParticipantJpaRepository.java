package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.domain.model.SplitBillParticipant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SplitBillParticipantJpaRepository extends JpaRepository<SplitBillParticipant, UUID> {
    List<SplitBillParticipant> findBySplitBillId(UUID splitBillId);
    List<SplitBillParticipant> findByAccountId(UUID accountId, Pageable pageable);
}
