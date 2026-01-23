package id.payu.transaction.adapter.persistence;

import id.payu.transaction.domain.model.SplitBill;
import id.payu.transaction.domain.model.SplitBillParticipant;
import id.payu.transaction.domain.port.out.SplitBillPersistencePort;
import id.payu.transaction.adapter.persistence.repository.SplitBillJpaRepository;
import id.payu.transaction.adapter.persistence.repository.SplitBillParticipantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SplitBillPersistenceAdapter implements SplitBillPersistencePort {

    private final SplitBillJpaRepository splitBillJpaRepository;
    private final SplitBillParticipantJpaRepository participantJpaRepository;

    @Override
    public SplitBill save(SplitBill splitBill) {
        return splitBillJpaRepository.save(splitBill);
    }

    @Override
    public SplitBillParticipant saveParticipant(SplitBillParticipant participant) {
        return participantJpaRepository.save(participant);
    }

    @Override
    public Optional<SplitBill> findById(UUID id) {
        return splitBillJpaRepository.findById(id);
    }

    @Override
    public Optional<SplitBill> findByReferenceNumber(String referenceNumber) {
        return splitBillJpaRepository.findByReferenceNumber(referenceNumber);
    }

    @Override
    public List<SplitBill> findByCreatorAccountId(UUID accountId, int page, int size) {
        return splitBillJpaRepository.findByCreatorAccountId(accountId, PageRequest.of(page, size));
    }

    @Override
    public List<SplitBillParticipant> findParticipantsBySplitBillId(UUID splitBillId) {
        return participantJpaRepository.findBySplitBillId(splitBillId);
    }

    @Override
    public Optional<SplitBillParticipant> findParticipantById(UUID participantId) {
        return participantJpaRepository.findById(participantId);
    }

    @Override
    public List<SplitBillParticipant> findByAccountId(UUID accountId, int page, int size) {
        return participantJpaRepository.findByAccountId(accountId, PageRequest.of(page, size));
    }

    @Override
    public void delete(SplitBill splitBill) {
        splitBillJpaRepository.delete(splitBill);
    }

    @Override
    public void deleteParticipant(UUID participantId) {
        participantJpaRepository.deleteById(participantId);
    }
}
