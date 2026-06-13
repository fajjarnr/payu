package id.payu.transaction.application.service;

import id.payu.transaction.adapter.persistence.entity.SplitBillEntity;
import id.payu.transaction.adapter.persistence.entity.SplitBillParticipantEntity;
import id.payu.transaction.domain.port.in.SplitBillUseCase;
import id.payu.transaction.domain.port.out.SplitBillPersistencePort;
import id.payu.transaction.domain.port.out.SplitBillEventPublisherPort;
import id.payu.transaction.dto.AddParticipantRequest;
import id.payu.transaction.dto.CreateSplitBillRequest;
import id.payu.transaction.dto.MakePaymentRequest;
import id.payu.transaction.dto.SplitBillResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import id.payu.transaction.domain.model.ParticipantStatus;
import id.payu.transaction.domain.model.SplitStatus;
import id.payu.transaction.domain.model.SplitType;

@Service
public class SplitBillService implements SplitBillUseCase {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SplitBillService.class);



    private final SplitBillPersistencePort persistencePort;
    private final SplitBillEventPublisherPort eventPublisher;

    public SplitBillService(SplitBillPersistencePort persistencePort,
                            SplitBillEventPublisherPort eventPublisher) {
        this.persistencePort = persistencePort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public SplitBillResponse createSplitBill(CreateSplitBillRequest request) {
        String referenceNumber = generateReferenceNumber();
        Instant now = Instant.now();

        // BUG-TXN-SPLITBILL-001: do NOT pre-assign the id. SplitBillEntity has
        // @Version Long version (added in V16 migration), so Spring Data's
        // isNew() checks version==null → true → calls em.persist() (not merge).
        // Letting Hibernate generate the UUID on persist ensures the entity
        // is treated as transient by both Spring Data AND Hibernate's
        // entityIsTransient check during cascade.
        //
        // Participants are saved EXPLICITLY (not via cascade) because the
        // unidirectional @JoinColumn OneToMany mapping doesn't reliably
        // set the split_bill_id FK on cascade insert with @GeneratedValue(UUID)
        // — Hibernate's batching flushes the participants before the parent's
        // generated UUID is propagated to the children's FK column.
        // See SplitBillParticipantEntity.splitBillId setter below.
        List<SplitBillParticipantEntity> participants = buildParticipants(request, now);
        SplitBillEntity splitBill = SplitBillEntity.builder()
                .referenceNumber(referenceNumber)
                .creatorAccountId(request.getCreatorAccountId())
                .totalAmount(request.getTotalAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "IDR")
                .title(request.getTitle())
                .description(request.getDescription())
                .splitType(request.getSplitType())
                .status(SplitStatus.DRAFT)
                .dueDate(request.getDueDate())
                .createdAt(now)
                .updatedAt(now)
                .build();

        splitBill = persistencePort.save(splitBill);
        final UUID parentId = splitBill.getId();
        for (SplitBillParticipantEntity p : participants) {
            p.setSplitBillId(parentId);
            persistencePort.saveParticipant(p);
        }
        splitBill.setParticipants(persistencePort.findParticipantsBySplitBillId(parentId));

        log.info("Split bill created, id: {}, reference: {}", splitBill.getId(), referenceNumber);
        eventPublisher.publishSplitBillCreated(splitBill);

        return mapToResponse(splitBill);
    }

    @Override
    public SplitBillResponse getSplitBill(UUID splitBillId) {
        SplitBillEntity splitBill = persistencePort.findById(splitBillId)
                .orElseThrow(() -> new IllegalArgumentException("Split bill not found"));

        splitBill.setParticipants(persistencePort.findParticipantsBySplitBillId(splitBillId));
        return mapToResponse(splitBill);
    }

    @Override
    public List<SplitBillEntity> getAccountSplitBills(UUID accountId, int page, int size) {
        return persistencePort.findByCreatorAccountId(accountId, page, size);
    }

    @Override
    @Transactional
    public SplitBillResponse updateSplitBill(UUID splitBillId, CreateSplitBillRequest request) {
        SplitBillEntity existing = persistencePort.findById(splitBillId)
                .orElseThrow(() -> new IllegalArgumentException("Split bill not found"));

        if (!existing.canBeModified()) {
            throw new IllegalStateException("Cannot update split bill in current status");
        }

        existing.setTotalAmount(request.getTotalAmount());
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setSplitType(request.getSplitType());
        existing.setDueDate(request.getDueDate());
        existing.setUpdatedAt(Instant.now());

        SplitBillEntity updated = persistencePort.save(existing);
        updated.setParticipants(persistencePort.findParticipantsBySplitBillId(updated.getId()));

        log.info("Split bill updated, id: {}", updated.getId());
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public SplitBillResponse cancelSplitBill(UUID splitBillId) {
        SplitBillEntity splitBill = persistencePort.findById(splitBillId)
                .orElseThrow(() -> new IllegalArgumentException("Split bill not found"));

        if (!splitBill.canBeCancelled()) {
            throw new IllegalStateException("Cannot cancel split bill in current status");
        }

        splitBill.setStatus(SplitStatus.CANCELLED);
        splitBill.setUpdatedAt(Instant.now());
        SplitBillEntity saved = persistencePort.save(splitBill);
        saved.setParticipants(persistencePort.findParticipantsBySplitBillId(saved.getId()));

        log.info("Split bill cancelled, id: {}", splitBillId);
        eventPublisher.publishSplitBillCancelled(saved);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public SplitBillResponse activateSplitBill(UUID splitBillId) {
        SplitBillEntity splitBill = persistencePort.findById(splitBillId)
                .orElseThrow(() -> new IllegalArgumentException("Split bill not found"));

        if (splitBill.getStatus() != SplitStatus.DRAFT) {
            throw new IllegalStateException("Can only activate draft split bills");
        }

        splitBill.setStatus(SplitStatus.ACTIVE);
        splitBill.setUpdatedAt(Instant.now());
        SplitBillEntity activated = persistencePort.save(splitBill);
        activated.setParticipants(persistencePort.findParticipantsBySplitBillId(activated.getId()));

        log.info("Split bill activated, id: {}", splitBillId);
        eventPublisher.publishSplitBillActivated(activated);

        return mapToResponse(activated);
    }

    @Override
    @Transactional
    public SplitBillResponse addParticipant(UUID splitBillId, AddParticipantRequest request) {
        SplitBillEntity splitBill = persistencePort.findById(splitBillId)
                .orElseThrow(() -> new IllegalArgumentException("Split bill not found"));

        if (!splitBill.canBeModified()) {
            throw new IllegalStateException("Cannot add participants in current status");
        }

        SplitBillParticipantEntity participant = SplitBillParticipantEntity.builder()
                .splitBillId(splitBillId)
                .accountId(request.getAccountId())
                .accountNumber(request.getAccountNumber())
                .accountName(request.getAccountName())
                .amountOwed(request.getAmountOwed() != null ? request.getAmountOwed() : BigDecimal.ZERO)
                .amountPaid(BigDecimal.ZERO)
                .status(ParticipantStatus.PENDING)
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
        SplitBillEntity splitBill = persistencePort.findById(splitBillId)
                .orElseThrow(() -> new IllegalArgumentException("Split bill not found"));

        SplitBillParticipantEntity participant = persistencePort.findParticipantById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found"));

        if (participant.getStatus() != ParticipantStatus.PENDING) {
            throw new IllegalStateException("Can only accept pending participants");
        }

        participant.setStatus(ParticipantStatus.ACCEPTED);
        participant.setUpdatedAt(Instant.now());
        persistencePort.saveParticipant(participant);

        splitBill.setStatus(SplitStatus.IN_PROGRESS);
        persistencePort.save(splitBill);

        splitBill.setParticipants(persistencePort.findParticipantsBySplitBillId(splitBillId));

        log.info("Participant accepted split bill, id: {}, participantId: {}", splitBillId, participantId);
        return mapToResponse(splitBill);
    }

    @Override
    @Transactional
    public SplitBillResponse declineSplitBill(UUID splitBillId, UUID participantId) {
        SplitBillEntity splitBill = persistencePort.findById(splitBillId)
                .orElseThrow(() -> new IllegalArgumentException("Split bill not found"));

        SplitBillParticipantEntity participant = persistencePort.findParticipantById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found"));

        if (participant.getStatus() != ParticipantStatus.PENDING) {
            throw new IllegalStateException("Can only decline pending participants");
        }

        participant.setStatus(ParticipantStatus.DECLINED);
        participant.setUpdatedAt(Instant.now());
        persistencePort.saveParticipant(participant);

        splitBill.setParticipants(persistencePort.findParticipantsBySplitBillId(splitBillId));

        log.info("Participant declined split bill, id: {}, participantId: {}", splitBillId, participantId);
        return mapToResponse(splitBill);
    }

    @Override
    @Transactional
    public SplitBillResponse makePayment(UUID splitBillId, UUID participantId, MakePaymentRequest request) {
        SplitBillEntity splitBill = persistencePort.findById(splitBillId)
                .orElseThrow(() -> new IllegalArgumentException("Split bill not found"));

        if (!splitBill.canAddPayment()) {
            throw new IllegalStateException("Cannot add payment in current status");
        }

        SplitBillParticipantEntity participant = persistencePort.findParticipantById(participantId)
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
            participant.setStatus(ParticipantStatus.SETTLED);
            participant.setSettledAt(Instant.now());
        } else {
            participant.setStatus(ParticipantStatus.PARTIALLY_PAID);
        }

        persistencePort.saveParticipant(participant);

        // BUG-BE-113: Refresh participants from DB before checking isFullyPaid()
        // The participant we just saved may be a different object than what's in splitBill.participants
        splitBill.setParticipants(persistencePort.findParticipantsBySplitBillId(splitBillId));

        if (splitBill.isFullyPaid()) {
            splitBill.setStatus(SplitStatus.COMPLETED);
            splitBill.setCompletedAt(Instant.now());
            splitBill.setUpdatedAt(Instant.now());
            persistencePort.save(splitBill);
            eventPublisher.publishSplitBillCompleted(splitBill);
        } else {
            splitBill.setStatus(SplitStatus.IN_PROGRESS);
            splitBill.setUpdatedAt(Instant.now());
            persistencePort.save(splitBill);
        }

        log.info("Payment made for split bill, id: {}, participantId: {}, amount: {}",
                splitBillId, participantId, request.getAmount());
        eventPublisher.publishPaymentMade(splitBill, participant, request.getAmount());

        return mapToResponse(splitBill);
    }

    /**
     * Settles a split bill after verifying all participants have fully paid.
     * BUG-BE-151: No longer force-completes without checking outstanding payments.
     */
    @Override
    @Transactional
    public SplitBillResponse settleSplitBill(UUID splitBillId) {
        SplitBillEntity splitBill = persistencePort.findById(splitBillId)
                .orElseThrow(() -> new IllegalArgumentException("Split bill not found"));

        if (splitBill.getStatus() == SplitStatus.COMPLETED) {
            throw new IllegalStateException("Split bill already completed");
        }

        if (splitBill.getStatus() == SplitStatus.CANCELLED) {
            throw new IllegalStateException("Cannot settle a cancelled split bill");
        }

        // BUG-BE-151: Verify all participants have paid before settling
        splitBill.setParticipants(persistencePort.findParticipantsBySplitBillId(splitBillId));
        if (!splitBill.isFullyPaid()) {
            throw new IllegalStateException(
                    "Cannot settle: outstanding payments remain. Total remaining: " + splitBill.getRemainingAmount());
        }

        splitBill.setStatus(SplitStatus.COMPLETED);
        splitBill.setCompletedAt(Instant.now());
        splitBill.setUpdatedAt(Instant.now());
        SplitBillEntity settled = persistencePort.save(splitBill);

        settled.setParticipants(persistencePort.findParticipantsBySplitBillId(settled.getId()));

        log.info("Split bill settled, id: {}", splitBillId);
        eventPublisher.publishSplitBillCompleted(settled);

        return mapToResponse(settled);
    }

    private List<SplitBillParticipantEntity> buildParticipants(CreateSplitBillRequest request, Instant now) {
        int participantCount = request.getParticipants().size();
        // BUG-BE-124: Fix EQUAL split rounding error
        // Calculate base amount per person, then assign remainder to last participant
        BigDecimal amountPerPerson = request.getTotalAmount()
                .divide(BigDecimal.valueOf(participantCount), 2, RoundingMode.DOWN);

        // Calculate remainder: totalAmount - (amountPerPerson * (count - 1))
        // Last participant gets the remainder to ensure total adds up exactly
        BigDecimal lastParticipantAmount = request.getTotalAmount()
                .subtract(amountPerPerson.multiply(BigDecimal.valueOf(participantCount - 1)));

        List<CreateSplitBillRequest.ParticipantRequest> participantList = request.getParticipants();
        return java.util.stream.IntStream.range(0, participantCount).mapToObj(i -> {
            CreateSplitBillRequest.ParticipantRequest p = participantList.get(i);
            boolean isLast = (i == participantCount - 1);

            BigDecimal amountOwed;
            if (request.getSplitType() == SplitType.EQUAL) {
                amountOwed = isLast ? lastParticipantAmount : amountPerPerson;
            } else {
                amountOwed = p.getAmountOwed() != null ? p.getAmountOwed() : BigDecimal.ZERO;
            }

            return SplitBillParticipantEntity.builder()
                    .splitBillId(null)
                    .accountId(p.getAccountId())
                    .accountNumber(p.getAccountNumber())
                    .accountName(p.getAccountName())
                    .amountOwed(amountOwed)
                    .amountPaid(BigDecimal.ZERO)
                    .status(ParticipantStatus.PENDING)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        }).toList();
    }

    private String generateReferenceNumber() {
        return "SPL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private SplitBillResponse mapToResponse(SplitBillEntity splitBill) {
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
