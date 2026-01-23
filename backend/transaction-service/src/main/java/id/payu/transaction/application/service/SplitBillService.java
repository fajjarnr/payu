package id.payu.transaction.application.service;

import id.payu.transaction.domain.model.SplitBill;
import id.payu.transaction.domain.model.SplitBillParticipant;
import id.payu.transaction.domain.port.in.SplitBillUseCase;
import id.payu.transaction.domain.port.out.SplitBillPersistencePort;
import id.payu.transaction.domain.port.out.SplitBillEventPublisherPort;
import id.payu.transaction.dto.AddParticipantRequest;
import id.payu.transaction.dto.CreateSplitBillRequest;
import id.payu.transaction.dto.MakePaymentRequest;
import id.payu.transaction.dto.SplitBillResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SplitBillService implements SplitBillUseCase {

    private final SplitBillPersistencePort persistencePort;
    private final SplitBillEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public SplitBillResponse createSplitBill(CreateSplitBillRequest request) {
        String referenceNumber = generateReferenceNumber();
        Instant now = Instant.now();

        SplitBill splitBill = SplitBill.builder()
                .id(UUID.randomUUID())
                .referenceNumber(referenceNumber)
                .creatorAccountId(request.getCreatorAccountId())
                .totalAmount(request.getTotalAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "IDR")
                .title(request.getTitle())
                .description(request.getDescription())
                .splitType(request.getSplitType())
                .status(SplitBill.SplitStatus.DRAFT)
                .dueDate(request.getDueDate())
                .createdAt(now)
                .updatedAt(now)
                .participants(buildParticipants(request, now))
                .build();

        splitBill = persistencePort.save(splitBill);
        splitBill.setParticipants(persistencePort.findParticipantsBySplitBillId(splitBill.getId()));

        log.info("Split bill created, id: {}, reference: {}", splitBill.getId(), referenceNumber);
        eventPublisher.publishSplitBillCreated(splitBill);

        return mapToResponse(splitBill);
    }

    @Override
    public SplitBillResponse getSplitBill(UUID splitBillId) {
        SplitBill splitBill = persistencePort.findById(splitBillId)
                .orElseThrow(() -> new IllegalArgumentException("Split bill not found"));

        splitBill.setParticipants(persistencePort.findParticipantsBySplitBillId(splitBillId));
        return mapToResponse(splitBill);
    }

    @Override
    public List<SplitBill> getAccountSplitBills(UUID accountId, int page, int size) {
        return persistencePort.findByCreatorAccountId(accountId, page, size);
    }

    @Override
    @Transactional
    public SplitBillResponse updateSplitBill(UUID splitBillId, CreateSplitBillRequest request) {
        SplitBill existing = persistencePort.findById(splitBillId)
                .orElseThrow(() -> new IllegalArgumentException("Split bill not found"));

        if (!existing.canBeCancelled()) {
            throw new IllegalStateException("Cannot update split bill in current status");
        }

        existing.setTotalAmount(request.getTotalAmount());
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setSplitType(request.getSplitType());
        existing.setDueDate(request.getDueDate());
        existing.setUpdatedAt(Instant.now());

        SplitBill updated = persistencePort.save(existing);
        updated.setParticipants(persistencePort.findParticipantsBySplitBillId(updated.getId()));

        log.info("Split bill updated, id: {}", updated.getId());
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void cancelSplitBill(UUID splitBillId) {
        SplitBill splitBill = persistencePort.findById(splitBillId)
                .orElseThrow(() -> new IllegalArgumentException("Split bill not found"));

        if (!splitBill.canBeCancelled()) {
            throw new IllegalStateException("Cannot cancel split bill in current status");
        }

        splitBill.setStatus(SplitBill.SplitStatus.CANCELLED);
        splitBill.setUpdatedAt(Instant.now());
        persistencePort.save(splitBill);

        log.info("Split bill cancelled, id: {}", splitBillId);
        eventPublisher.publishSplitBillCancelled(splitBill);
    }

    @Override
    @Transactional
    public SplitBillResponse activateSplitBill(UUID splitBillId) {
        SplitBill splitBill = persistencePort.findById(splitBillId)
                .orElseThrow(() -> new IllegalArgumentException("Split bill not found"));

        if (splitBill.getStatus() != SplitBill.SplitStatus.DRAFT) {
            throw new IllegalStateException("Can only activate draft split bills");
        }

        splitBill.setStatus(SplitBill.SplitStatus.ACTIVE);
        splitBill.setUpdatedAt(Instant.now());
        SplitBill activated = persistencePort.save(splitBill);
        activated.setParticipants(persistencePort.findParticipantsBySplitBillId(activated.getId()));

        log.info("Split bill activated, id: {}", splitBillId);
        eventPublisher.publishSplitBillActivated(activated);

        return mapToResponse(activated);
    }

    @Override
    @Transactional
    public SplitBillResponse addParticipant(UUID splitBillId, AddParticipantRequest request) {
        SplitBill splitBill = persistencePort.findById(splitBillId)
                .orElseThrow(() -> new IllegalArgumentException("Split bill not found"));

        if (!splitBill.canBeCancelled()) {
            throw new IllegalStateException("Cannot add participants in current status");
        }

        SplitBillParticipant participant = SplitBillParticipant.builder()
                .id(UUID.randomUUID())
                .splitBillId(splitBillId)
                .accountId(request.getAccountId())
                .accountNumber(request.getAccountNumber())
                .accountName(request.getAccountName())
                .amountOwed(request.getAmountOwed() != null ? request.getAmountOwed() : BigDecimal.ZERO)
                .amountPaid(BigDecimal.ZERO)
                .status(SplitBillParticipant.ParticipantStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        persistencePort.saveParticipant(participant);
        splitBill.setParticipants(persistencePort.findParticipantsBySplitBillId(splitBillId));

        log.info("Participant added to split bill, id: {}, participantId: {}", splitBillId, participant.getId());
        eventPublisher.publishParticipantAdded(splitBill, participant);

        return mapToResponse(splitBill);
    }

    @Override
    @Transactional
    public SplitBillResponse acceptSplitBill(UUID splitBillId, UUID participantId) {
        SplitBill splitBill = persistencePort.findById(splitBillId)
                .orElseThrow(() -> new IllegalArgumentException("Split bill not found"));

        SplitBillParticipant participant = persistencePort.findParticipantById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found"));

        if (participant.getStatus() != SplitBillParticipant.ParticipantStatus.PENDING) {
            throw new IllegalStateException("Can only accept pending participants");
        }

        participant.setStatus(SplitBillParticipant.ParticipantStatus.ACCEPTED);
        participant.setUpdatedAt(Instant.now());
        persistencePort.saveParticipant(participant);

        splitBill.setStatus(SplitBill.SplitStatus.IN_PROGRESS);
        persistencePort.save(splitBill);

        splitBill.setParticipants(persistencePort.findParticipantsBySplitBillId(splitBillId));

        log.info("Participant accepted split bill, id: {}, participantId: {}", splitBillId, participantId);
        return mapToResponse(splitBill);
    }

    @Override
    @Transactional
    public SplitBillResponse declineSplitBill(UUID splitBillId, UUID participantId) {
        SplitBill splitBill = persistencePort.findById(splitBillId)
                .orElseThrow(() -> new IllegalArgumentException("Split bill not found"));

        SplitBillParticipant participant = persistencePort.findParticipantById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found"));

        if (participant.getStatus() != SplitBillParticipant.ParticipantStatus.PENDING) {
            throw new IllegalStateException("Can only decline pending participants");
        }

        participant.setStatus(SplitBillParticipant.ParticipantStatus.DECLINED);
        participant.setUpdatedAt(Instant.now());
        persistencePort.saveParticipant(participant);

        splitBill.setParticipants(persistencePort.findParticipantsBySplitBillId(splitBillId));

        log.info("Participant declined split bill, id: {}, participantId: {}", splitBillId, participantId);
        return mapToResponse(splitBill);
    }

    @Override
    @Transactional
    public SplitBillResponse makePayment(UUID splitBillId, UUID participantId, MakePaymentRequest request) {
        SplitBill splitBill = persistencePort.findById(splitBillId)
                .orElseThrow(() -> new IllegalArgumentException("Split bill not found"));

        if (!splitBill.canAddPayment()) {
            throw new IllegalStateException("Cannot add payment in current status");
        }

        SplitBillParticipant participant = persistencePort.findParticipantById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found"));

        if (!participant.canMakePayment()) {
            throw new IllegalStateException("Cannot make payment in current status");
        }

        BigDecimal newAmountPaid = participant.getAmountPaid().add(request.getAmount());
        if (newAmountPaid.compareTo(participant.getAmountOwed()) > 0) {
            throw new IllegalArgumentException("Payment exceeds amount owed");
        }

        participant.setAmountPaid(newAmountPaid);
        participant.setUpdatedAt(Instant.now());

        if (participant.isFullyPaid()) {
            participant.setStatus(SplitBillParticipant.ParticipantStatus.SETTLED);
            participant.setSettledAt(Instant.now());
        } else {
            participant.setStatus(SplitBillParticipant.ParticipantStatus.PARTIALLY_PAID);
        }

        persistencePort.saveParticipant(participant);

        if (splitBill.isFullyPaid()) {
            splitBill.setStatus(SplitBill.SplitStatus.COMPLETED);
            splitBill.setCompletedAt(Instant.now());
            splitBill.setUpdatedAt(Instant.now());
            persistencePort.save(splitBill);
            eventPublisher.publishSplitBillCompleted(splitBill);
        } else {
            splitBill.setStatus(SplitBill.SplitStatus.IN_PROGRESS);
            splitBill.setUpdatedAt(Instant.now());
            persistencePort.save(splitBill);
        }

        splitBill.setParticipants(persistencePort.findParticipantsBySplitBillId(splitBillId));

        log.info("Payment made for split bill, id: {}, participantId: {}, amount: {}",
                splitBillId, participantId, request.getAmount());
        eventPublisher.publishPaymentMade(splitBill, participant, request.getAmount());

        return mapToResponse(splitBill);
    }

    @Override
    @Transactional
    public SplitBillResponse settleSplitBill(UUID splitBillId) {
        SplitBill splitBill = persistencePort.findById(splitBillId)
                .orElseThrow(() -> new IllegalArgumentException("Split bill not found"));

        if (splitBill.getStatus() == SplitBill.SplitStatus.COMPLETED) {
            throw new IllegalStateException("Split bill already completed");
        }

        splitBill.setStatus(SplitBill.SplitStatus.COMPLETED);
        splitBill.setCompletedAt(Instant.now());
        splitBill.setUpdatedAt(Instant.now());
        SplitBill settled = persistencePort.save(splitBill);

        settled.setParticipants(persistencePort.findParticipantsBySplitBillId(settled.getId()));

        log.info("Split bill settled, id: {}", splitBillId);
        eventPublisher.publishSplitBillCompleted(settled);

        return mapToResponse(settled);
    }

    private List<SplitBillParticipant> buildParticipants(CreateSplitBillRequest request, Instant now) {
        BigDecimal amountPerPerson = request.getTotalAmount()
                .divide(BigDecimal.valueOf(request.getParticipants().size()), 2, RoundingMode.HALF_UP);

        return request.getParticipants().stream().map(p -> {
            BigDecimal amountOwed = request.getSplitType() == SplitBill.SplitType.EQUAL
                    ? amountPerPerson
                    : (p.getAmountOwed() != null ? p.getAmountOwed() : BigDecimal.ZERO);

            return SplitBillParticipant.builder()
                    .id(UUID.randomUUID())
                    .splitBillId(null)
                    .accountId(p.getAccountId())
                    .accountNumber(p.getAccountNumber())
                    .accountName(p.getAccountName())
                    .amountOwed(amountOwed)
                    .amountPaid(BigDecimal.ZERO)
                    .status(SplitBillParticipant.ParticipantStatus.PENDING)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        }).toList();
    }

    private String generateReferenceNumber() {
        return "SPL" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    private SplitBillResponse mapToResponse(SplitBill splitBill) {
        return SplitBillResponse.builder()
                .id(splitBill.getId())
                .referenceNumber(splitBill.getReferenceNumber())
                .creatorAccountId(splitBill.getCreatorAccountId())
                .totalAmount(splitBill.getTotalAmount())
                .currency(splitBill.getCurrency())
                .title(splitBill.getTitle())
                .description(splitBill.getDescription())
                .splitType(splitBill.getSplitType().name())
                .status(splitBill.getStatus().name())
                .dueDate(splitBill.getDueDate())
                .participants(splitBill.getParticipants().stream()
                        .map(p -> SplitBillResponse.ParticipantResponse.builder()
                                .id(p.getId())
                                .accountId(p.getAccountId())
                                .accountNumber(p.getAccountNumber())
                                .accountName(p.getAccountName())
                                .amountOwed(p.getAmountOwed())
                                .amountPaid(p.getAmountPaid())
                                .remainingAmount(p.getRemainingAmount())
                                .status(p.getStatus().name())
                                .settledAt(p.getSettledAt())
                                .createdAt(p.getCreatedAt())
                                .updatedAt(p.getUpdatedAt())
                                .build())
                        .toList())
                .totalPaid(splitBill.getTotalPaid())
                .remainingAmount(splitBill.getRemainingAmount())
                .createdAt(splitBill.getCreatedAt())
                .updatedAt(splitBill.getUpdatedAt())
                .completedAt(splitBill.getCompletedAt())
                .build();
    }
}
