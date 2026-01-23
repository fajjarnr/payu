package id.payu.transaction.domain.port.out;

import id.payu.transaction.domain.model.SplitBill;
import id.payu.transaction.domain.model.SplitBillParticipant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SplitBillPersistencePort {
    SplitBill save(SplitBill splitBill);

    SplitBillParticipant saveParticipant(SplitBillParticipant participant);

    Optional<SplitBill> findById(UUID id);

    Optional<SplitBill> findByReferenceNumber(String referenceNumber);

    List<SplitBill> findByCreatorAccountId(UUID accountId, int page, int size);

    List<SplitBillParticipant> findParticipantsBySplitBillId(UUID splitBillId);

    Optional<SplitBillParticipant> findParticipantById(UUID participantId);

    List<SplitBillParticipant> findByAccountId(UUID accountId, int page, int size);

    void delete(SplitBill splitBill);

    void deleteParticipant(UUID participantId);
}
