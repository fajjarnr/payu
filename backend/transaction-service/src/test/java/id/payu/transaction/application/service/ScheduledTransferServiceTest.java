package id.payu.transaction.application.service;

import id.payu.transaction.domain.model.ScheduledTransfer;
import id.payu.transaction.domain.model.Transaction;
import id.payu.transaction.domain.port.in.TransactionUseCase;
import id.payu.transaction.domain.port.out.ScheduledTransferPersistencePort;
import id.payu.transaction.dto.CreateScheduledTransferRequest;
import id.payu.transaction.dto.InitiateTransferRequest;
import id.payu.transaction.dto.InitiateTransferResponse;
import id.payu.transaction.dto.ScheduledTransferResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledTransferServiceTest {

    @Mock
    private ScheduledTransferPersistencePort persistencePort;

    @Mock
    private TransactionUseCase transactionUseCase;

    @InjectMocks
    private ScheduledTransferService service;

    private CreateScheduledTransferRequest request;
    private ScheduledTransfer scheduledTransfer;

    @BeforeEach
    void setUp() {
        request = CreateScheduledTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("100000"))
                .currency("IDR")
                .description("Monthly transfer")
                .transferType(Transaction.TransactionType.INTERNAL_TRANSFER)
                .scheduleType(ScheduledTransfer.ScheduleType.RECURRING_MONTHLY)
                .startDate(Instant.now())
                .dayOfMonth(1)
                .occurrenceCount(12)
                .build();

        scheduledTransfer = ScheduledTransfer.builder()
                .id(UUID.randomUUID())
                .referenceNumber("SCH1234567890")
                .senderAccountId(request.getSenderAccountId())
                .recipientAccountNumber(request.getRecipientAccountNumber())
                .transferType(request.getTransferType())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .description(request.getDescription())
                .scheduleType(request.getScheduleType())
                .startDate(request.getStartDate())
                .dayOfMonth(request.getDayOfMonth())
                .occurrenceCount(request.getOccurrenceCount())
                .executedCount(0)
                .status(ScheduledTransfer.ScheduledStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void createScheduledTransfer_Success() {
        when(persistencePort.save(any(ScheduledTransfer.class))).thenReturn(scheduledTransfer);

        ScheduledTransferResponse response = service.createScheduledTransfer(request);

        assertNotNull(response);
        assertEquals("SCH1234567890", response.getReferenceNumber());
        assertEquals(request.getSenderAccountId(), response.getSenderAccountId());
        assertEquals("ACTIVE", response.getStatus());
        verify(persistencePort, times(1)).save(any(ScheduledTransfer.class));
    }

    @Test
    void getScheduledTransfer_Success() {
        UUID id = scheduledTransfer.getId();
        when(persistencePort.findById(id)).thenReturn(Optional.of(scheduledTransfer));

        ScheduledTransferResponse response = service.getScheduledTransfer(id);

        assertNotNull(response);
        assertEquals(id, response.getId());
        verify(persistencePort, times(1)).findById(id);
    }

    @Test
    void getScheduledTransfer_NotFound() {
        UUID id = UUID.randomUUID();
        when(persistencePort.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getScheduledTransfer(id));
    }

    @Test
    void cancelScheduledTransfer_Success() {
        UUID id = scheduledTransfer.getId();
        when(persistencePort.findById(id)).thenReturn(Optional.of(scheduledTransfer));
        when(persistencePort.save(any(ScheduledTransfer.class))).thenReturn(scheduledTransfer);

        service.cancelScheduledTransfer(id);

        verify(persistencePort, times(1)).findById(id);
        verify(persistencePort, times(1)).save(any(ScheduledTransfer.class));
    }

    @Test
    void pauseScheduledTransfer_Success() {
        UUID id = scheduledTransfer.getId();
        when(persistencePort.findById(id)).thenReturn(Optional.of(scheduledTransfer));
        when(persistencePort.save(any(ScheduledTransfer.class))).thenReturn(scheduledTransfer);

        service.pauseScheduledTransfer(id);

        verify(persistencePort, times(1)).findById(id);
        verify(persistencePort, times(1)).save(any(ScheduledTransfer.class));
    }

    @Test
    void pauseScheduledTransfer_NotActive() {
        scheduledTransfer.setStatus(ScheduledTransfer.ScheduledStatus.PAUSED);
        UUID id = scheduledTransfer.getId();
        when(persistencePort.findById(id)).thenReturn(Optional.of(scheduledTransfer));

        assertThrows(IllegalStateException.class, () -> service.pauseScheduledTransfer(id));
    }

    @Test
    void resumeScheduledTransfer_Success() {
        scheduledTransfer.setStatus(ScheduledTransfer.ScheduledStatus.PAUSED);
        UUID id = scheduledTransfer.getId();
        when(persistencePort.findById(id)).thenReturn(Optional.of(scheduledTransfer));
        when(persistencePort.save(any(ScheduledTransfer.class))).thenReturn(scheduledTransfer);

        service.resumeScheduledTransfer(id);

        verify(persistencePort, times(1)).findById(id);
        verify(persistencePort, times(1)).save(any(ScheduledTransfer.class));
    }

    @Test
    void resumeScheduledTransfer_NotPaused() {
        UUID id = scheduledTransfer.getId();
        when(persistencePort.findById(id)).thenReturn(Optional.of(scheduledTransfer));

        assertThrows(IllegalStateException.class, () -> service.resumeScheduledTransfer(id));
    }

    @Test
    void getAccountScheduledTransfers_Success() {
        UUID accountId = UUID.randomUUID();
        List<ScheduledTransfer> transfers = List.of(scheduledTransfer);
        when(persistencePort.findBySenderAccountId(accountId)).thenReturn(transfers);

        List<ScheduledTransfer> result = service.getAccountScheduledTransfers(accountId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(persistencePort, times(1)).findBySenderAccountId(accountId);
    }

    @Test
    void processDueScheduledTransfer_Success() {
        scheduledTransfer.setNextExecutionDate(Instant.now().minusSeconds(60));
        UUID transactionId = UUID.randomUUID();
        InitiateTransferResponse transactionResponse = InitiateTransferResponse.builder()
                .transactionId(transactionId)
                .referenceNumber("TXN123")
                .status("PENDING")
                .build();

        when(transactionUseCase.initiateTransfer(any(InitiateTransferRequest.class))).thenReturn(transactionResponse);
        when(persistencePort.save(any(ScheduledTransfer.class))).thenReturn(scheduledTransfer);

        service.processDueScheduledTransfer(scheduledTransfer);

        verify(transactionUseCase, times(1)).initiateTransfer(any(InitiateTransferRequest.class));
        verify(persistencePort, times(1)).save(any(ScheduledTransfer.class));
    }

    @Test
    void processDueScheduledTransfer_NotDue() {
        scheduledTransfer.setNextExecutionDate(Instant.now().plusSeconds(3600));

        service.processDueScheduledTransfer(scheduledTransfer);

        verify(transactionUseCase, never()).initiateTransfer(any(InitiateTransferRequest.class));
        verify(persistencePort, never()).save(any(ScheduledTransfer.class));
    }

    @Test
    void updateScheduledTransfer_Success() {
        UUID id = scheduledTransfer.getId();
        CreateScheduledTransferRequest updateRequest = CreateScheduledTransferRequest.builder()
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("0987654321")
                .amount(new BigDecimal("200000"))
                .currency("IDR")
                .description("Updated transfer")
                .transferType(Transaction.TransactionType.INTERNAL_TRANSFER)
                .scheduleType(ScheduledTransfer.ScheduleType.RECURRING_MONTHLY)
                .startDate(Instant.now().plusSeconds(300))
                .dayOfMonth(15)
                .build();

        when(persistencePort.findById(id)).thenReturn(Optional.of(scheduledTransfer));
        when(persistencePort.save(any(ScheduledTransfer.class))).thenReturn(scheduledTransfer);

        ScheduledTransferResponse response = service.updateScheduledTransfer(id, updateRequest);

        assertNotNull(response);
        verify(persistencePort, times(1)).findById(id);
        verify(persistencePort, times(1)).save(any(ScheduledTransfer.class));
    }

    @Test
    void updateScheduledTransfer_Completed_ShouldFail() {
        scheduledTransfer.setStatus(ScheduledTransfer.ScheduledStatus.COMPLETED);
        UUID id = scheduledTransfer.getId();
        when(persistencePort.findById(id)).thenReturn(Optional.of(scheduledTransfer));

        assertThrows(IllegalStateException.class, () -> service.updateScheduledTransfer(id, request));
    }
}
