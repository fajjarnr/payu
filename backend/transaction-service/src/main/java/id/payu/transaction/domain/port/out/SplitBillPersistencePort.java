package id.payu.transaction.domain.port.out;

import id.payu.transaction.adapter.persistence.entity.SplitBillEntity;
import id.payu.transaction.adapter.persistence.entity.SplitBillParticipantEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SplitBillPersistencePort {
    SplitBillEntity save(SplitBillEntity splitBill);

    SplitBillParticipantEntity saveParticipant(SplitBillParticipantEntity participant);

    Optional<SplitBillEntity> findById(UUID id);

    Optional<SplitBillEntity> findByReferenceNumber(String referenceNumber);

    List<SplitBillEntity> findByCreatorAccountId(UUID accountId, int page, int size);

    List<SplitBillParticipantEntity> findParticipantsBySplitBillId(UUID splitBillId);

    Optional<SplitBillParticipantEntity> findParticipantById(UUID participantId);

    List<SplitBillParticipantEntity> findByAccountId(UUID accountId, int page, int size);

    void delete(SplitBillEntity splitBill);

    void deleteParticipant(UUID participantId);
}
