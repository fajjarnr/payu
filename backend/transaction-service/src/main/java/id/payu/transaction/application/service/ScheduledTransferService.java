package id.payu.transaction.application.service;

import id.payu.transaction.domain.model.ScheduledTransfer;
import id.payu.transaction.domain.port.in.ScheduledTransferUseCase;
import id.payu.transaction.domain.port.in.TransactionUseCase;
import id.payu.transaction.domain.port.out.ScheduledTransferPersistencePort;
import id.payu.transaction.dto.CreateScheduledTransferRequest;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.dto.ScheduledTransferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTransferService implements ScheduledTransferUseCase {

    private final ScheduledTransferPersistencePort persistencePort;
    private final TransactionUseCase transactionUseCase;

    @Override
    @Transactional
    public ScheduledTransferResponse createScheduledTransfer(CreateScheduledTransferRequest request) {
        String referenceNumber = generateReferenceNumber();
        Instant nextExecutionDate = calculateNextExecutionDate(request.getScheduleType(), request.getStartDate(),
                request.getFrequencyDays(), request.getDayOfMonth());

        ScheduledTransfer scheduledTransfer = ScheduledTransfer.builder()
                .id(UUID.randomUUID())
                .referenceNumber(referenceNumber)
                .senderAccountId(request.getSenderAccountId())
                .recipientAccountNumber(request.getRecipientAccountNumber())
                .transferType(request.getTransferType())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "IDR")
                .description(request.getDescription())
                .scheduleType(request.getScheduleType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .nextExecutionDate(nextExecutionDate)
                .frequencyDays(request.getFrequencyDays())
                .dayOfMonth(request.getDayOfMonth())
                .occurrenceCount(request.getOccurrenceCount())
                .executedCount(0)
                .status(ScheduledTransfer.ScheduledStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        scheduledTransfer = persistencePort.save(scheduledTransfer);
        log.info("Scheduled transfer created, id: {}, reference: {}, nextExecution: {}",
                scheduledTransfer.getId(), referenceNumber, nextExecutionDate);

        return mapToResponse(scheduledTransfer);
    }

    @Override
    public ScheduledTransferResponse getScheduledTransfer(UUID id) {
        ScheduledTransfer scheduledTransfer = persistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled transfer not found"));
        return mapToResponse(scheduledTransfer);
    }

    @Override
    @Transactional
    public ScheduledTransferResponse updateScheduledTransfer(UUID id, CreateScheduledTransferRequest request) {
        ScheduledTransfer existing = persistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled transfer not found"));

        if (existing.getStatus() == ScheduledTransfer.ScheduledStatus.COMPLETED ||
            existing.getStatus() == ScheduledTransfer.ScheduledStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update completed or cancelled scheduled transfer");
        }

        Instant nextExecutionDate = calculateNextExecutionDate(request.getScheduleType(), request.getStartDate(),
                request.getFrequencyDays(), request.getDayOfMonth());

        existing.setRecipientAccountNumber(request.getRecipientAccountNumber());
        existing.setTransferType(request.getTransferType());
        existing.setAmount(request.getAmount());
        existing.setDescription(request.getDescription());
        existing.setScheduleType(request.getScheduleType());
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());
        existing.setNextExecutionDate(nextExecutionDate);
        existing.setFrequencyDays(request.getFrequencyDays());
        existing.setDayOfMonth(request.getDayOfMonth());
        existing.setOccurrenceCount(request.getOccurrenceCount());

        ScheduledTransfer updated = persistencePort.save(existing);
        log.info("Scheduled transfer updated, id: {}", updated.getId());

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void cancelScheduledTransfer(UUID id) {
        ScheduledTransfer scheduledTransfer = persistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled transfer not found"));

        scheduledTransfer.setStatus(ScheduledTransfer.ScheduledStatus.CANCELLED);
        persistencePort.save(scheduledTransfer);

        log.info("Scheduled transfer cancelled, id: {}", id);
    }

    @Override
    @Transactional
    public void pauseScheduledTransfer(UUID id) {
        ScheduledTransfer scheduledTransfer = persistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled transfer not found"));

        if (scheduledTransfer.getStatus() != ScheduledTransfer.ScheduledStatus.ACTIVE) {
            throw new IllegalStateException("Can only pause active scheduled transfers");
        }

        scheduledTransfer.setStatus(ScheduledTransfer.ScheduledStatus.PAUSED);
        persistencePort.save(scheduledTransfer);

        log.info("Scheduled transfer paused, id: {}", id);
    }

    @Override
    @Transactional
    public void resumeScheduledTransfer(UUID id) {
        ScheduledTransfer scheduledTransfer = persistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled transfer not found"));

        if (scheduledTransfer.getStatus() != ScheduledTransfer.ScheduledStatus.PAUSED) {
            throw new IllegalStateException("Can only resume paused scheduled transfers");
        }

        scheduledTransfer.setStatus(ScheduledTransfer.ScheduledStatus.ACTIVE);
        persistencePort.save(scheduledTransfer);

        log.info("Scheduled transfer resumed, id: {}", id);
    }

    @Override
    public List<ScheduledTransfer> getAccountScheduledTransfers(UUID accountId) {
        return persistencePort.findBySenderAccountId(accountId);
    }

    @Transactional
    public void processDueScheduledTransfer(ScheduledTransfer scheduledTransfer) {
        if (!scheduledTransfer.isDueForExecution()) {
            return;
        }

        try {
            InitiateTransferRequest request = InitiateTransferRequest.builder()
                    .senderAccountId(scheduledTransfer.getSenderAccountId())
                    .recipientAccountNumber(scheduledTransfer.getRecipientAccountNumber())
                    .amount(scheduledTransfer.getAmount())
                    .currency(scheduledTransfer.getCurrency())
                    .description(scheduledTransfer.getDescription())
                    .type(InitiateTransferRequest.TransactionType.valueOf(scheduledTransfer.getTransferType().name()))
                    .build();

            var response = transactionUseCase.initiateTransfer(
                    request,
                    scheduledTransfer.getSenderAccountId().toString());

            scheduledTransfer.setExecutedCount(scheduledTransfer.getExecutedCount() + 1);
            scheduledTransfer.setLastTransactionId(response.transactionId());

            if (scheduledTransfer.isCompleted()) {
                scheduledTransfer.setStatus(ScheduledTransfer.ScheduledStatus.COMPLETED);
            } else {
                Instant nextExecution = calculateNextExecutionDate(
                        scheduledTransfer.getScheduleType(),
                        scheduledTransfer.getNextExecutionDate(),
                        scheduledTransfer.getFrequencyDays(),
                        scheduledTransfer.getDayOfMonth());
                scheduledTransfer.setNextExecutionDate(nextExecution);
            }

            persistencePort.save(scheduledTransfer);

            log.info("Scheduled transfer executed successfully, id: {}, transactionId: {}, executedCount: {}",
                    scheduledTransfer.getId(), response.transactionId(), scheduledTransfer.getExecutedCount());

        } catch (Exception e) {
            scheduledTransfer.setStatus(ScheduledTransfer.ScheduledStatus.FAILED);
            scheduledTransfer.setFailureReason(e.getMessage());
            persistencePort.save(scheduledTransfer);

            log.error("Scheduled transfer execution failed, id: {}, error: {}",
                    scheduledTransfer.getId(), e.getMessage());
        }
    }

    private String generateReferenceNumber() {
        return "SCH" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    private Instant calculateNextExecutionDate(ScheduledTransfer.ScheduleType scheduleType, Instant baseDate,
                                               Integer frequencyDays, Integer dayOfMonth) {
        return switch (scheduleType) {
            case ONE_TIME -> baseDate;
            case RECURRING_DAILY -> baseDate.plusSeconds(86400);
            case RECURRING_WEEKLY -> baseDate.plusSeconds(604800);
            case RECURRING_MONTHLY -> {
                if (dayOfMonth != null && dayOfMonth > 0 && dayOfMonth <= 31) {
                    yield baseDate.atZone(java.time.ZoneId.of("Asia/Jakarta"))
                            .withDayOfMonth(Math.min(dayOfMonth, baseDate.atZone(java.time.ZoneId.of("Asia/Jakarta"))
                                    .toLocalDate().lengthOfMonth()))
                            .plusMonths(1)
                            .toInstant();
                }
                yield baseDate.plusSeconds(2629800);
            }
            case RECURRING_CUSTOM -> frequencyDays != null
                    ? baseDate.plusSeconds(frequencyDays * 86400L)
                    : baseDate.plusSeconds(2629800);
        };
    }

    private ScheduledTransferResponse mapToResponse(ScheduledTransfer scheduledTransfer) {
        return ScheduledTransferResponse.builder()
                .id(scheduledTransfer.getId())
                .referenceNumber(scheduledTransfer.getReferenceNumber())
                .senderAccountId(scheduledTransfer.getSenderAccountId())
                .recipientAccountNumber(scheduledTransfer.getRecipientAccountNumber())
                .recipientAccountId(scheduledTransfer.getRecipientAccountId())
                .transferType(scheduledTransfer.getTransferType().name())
                .amount(scheduledTransfer.getAmount())
                .currency(scheduledTransfer.getCurrency())
                .description(scheduledTransfer.getDescription())
                .scheduleType(scheduledTransfer.getScheduleType().name())
                .startDate(scheduledTransfer.getStartDate())
                .endDate(scheduledTransfer.getEndDate())
                .nextExecutionDate(scheduledTransfer.getNextExecutionDate())
                .frequencyDays(scheduledTransfer.getFrequencyDays())
                .dayOfMonth(scheduledTransfer.getDayOfMonth())
                .occurrenceCount(scheduledTransfer.getOccurrenceCount())
                .executedCount(scheduledTransfer.getExecutedCount())
                .status(scheduledTransfer.getStatus().name())
                .failureReason(scheduledTransfer.getFailureReason())
                .lastTransactionId(scheduledTransfer.getLastTransactionId())
                .createdAt(scheduledTransfer.getCreatedAt())
                .updatedAt(scheduledTransfer.getUpdatedAt())
                .build();
    }
}
