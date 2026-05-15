package id.payu.transaction.adapter.persistence.repository;

import id.payu.transaction.adapter.persistence.entity.SplitBillParticipantEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SplitBillParticipantJpaRepository extends JpaRepository<SplitBillParticipantEntity, UUID> {
    List<SplitBillParticipantEntity> findBySplitBillId(UUID splitBillId);
    List<SplitBillParticipantEntity> findByAccountId(UUID accountId, Pageable pageable);
}
