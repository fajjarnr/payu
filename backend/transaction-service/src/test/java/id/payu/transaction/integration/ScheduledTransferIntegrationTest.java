package id.payu.transaction.integration;

import id.payu.transaction.domain.model.ScheduledTransfer;
import id.payu.transaction.adapter.persistence.ScheduledTransferPersistenceAdapter;
import id.payu.transaction.adapter.persistence.repository.ScheduledTransferJpaRepository;
import id.payu.transaction.application.service.ScheduledTransferService;
import id.payu.transaction.dto.CreateScheduledTransferRequest;
import id.payu.transaction.dto.InitiateTransferRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScheduledTransferIntegrationTest {

    @Autowired
    private ScheduledTransferService scheduledTransferService;

    @Autowired
    private ScheduledTransferPersistenceAdapter persistenceAdapter;

    @Autowired
    private ScheduledTransferJpaRepository repository;

    private CreateScheduledTransferRequest request;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        request = CreateScheduledTransferRequest.builder()
                .senderAccountId(accountId)
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("100000"))
                .currency("IDR")
                .description("Monthly transfer")
                .transferType(InitiateTransferRequest.TransactionType.INTERNAL_TRANSFER)
                .scheduleType(ScheduledTransfer.ScheduleType.RECURRING_MONTHLY)
                .startDate(Instant.now().plusSeconds(300))
                .dayOfMonth(1)
                .occurrenceCount(12)
                .build();

        repository.deleteAll();
    }

    @Test
    void createAndRetrieveScheduledTransfer() {
        var response = scheduledTransferService.createScheduledTransfer(request);

        assertNotNull(response.getId());
        assertNotNull(response.getReferenceNumber());
        assertEquals(accountId, response.getSenderAccountId());
        assertEquals("RECURRING_MONTHLY", response.getScheduleType());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(0, response.getExecutedCount());
        assertEquals(12, response.getOccurrenceCount());
    }

    @Test
    void getAccountScheduledTransfers() {
        scheduledTransferService.createScheduledTransfer(request);

        List<ScheduledTransfer> transfers = scheduledTransferService.getAccountScheduledTransfers(accountId);

        assertNotNull(transfers);
        assertEquals(1, transfers.size());
    }

    @Test
    void cancelScheduledTransfer() {
        var created = scheduledTransferService.createScheduledTransfer(request);
        UUID id = created.getId();

        scheduledTransferService.cancelScheduledTransfer(id);

        var cancelled = scheduledTransferService.getScheduledTransfer(id);
        assertEquals("CANCELLED", cancelled.getStatus());
    }

    @Test
    void pauseAndResumeScheduledTransfer() {
        var created = scheduledTransferService.createScheduledTransfer(request);
        UUID id = created.getId();

        scheduledTransferService.pauseScheduledTransfer(id);

        var paused = scheduledTransferService.getScheduledTransfer(id);
        assertEquals("PAUSED", paused.getStatus());

        scheduledTransferService.resumeScheduledTransfer(id);

        var resumed = scheduledTransferService.getScheduledTransfer(id);
        assertEquals("ACTIVE", resumed.getStatus());
    }

    @Test
    void updateScheduledTransfer() {
        var created = scheduledTransferService.createScheduledTransfer(request);
        UUID id = created.getId();

        CreateScheduledTransferRequest updateRequest = CreateScheduledTransferRequest.builder()
                .senderAccountId(accountId)
                .recipientAccountNumber("0987654321")
                .amount(new BigDecimal("200000"))
                .currency("IDR")
                .description("Updated monthly transfer")
                .transferType(InitiateTransferRequest.TransactionType.INTERNAL_TRANSFER)
                .scheduleType(ScheduledTransfer.ScheduleType.RECURRING_MONTHLY)
                .startDate(Instant.now().plusSeconds(300))
                .dayOfMonth(15)
                .occurrenceCount(6)
                .build();

        var updated = scheduledTransferService.updateScheduledTransfer(id, updateRequest);

        assertEquals("0987654321", updated.getRecipientAccountNumber());
        assertEquals(new BigDecimal("200000"), updated.getAmount());
        assertEquals("Updated monthly transfer", updated.getDescription());
        assertEquals(15, updated.getDayOfMonth());
        assertEquals(6, updated.getOccurrenceCount());
    }

    @Test
    void oneTimeScheduledTransfer() {
        CreateScheduledTransferRequest oneTimeRequest = CreateScheduledTransferRequest.builder()
                .senderAccountId(accountId)
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("50000"))
                .currency("IDR")
                .description("One time transfer")
                .transferType(InitiateTransferRequest.TransactionType.INTERNAL_TRANSFER)
                .scheduleType(ScheduledTransfer.ScheduleType.ONE_TIME)
                .startDate(Instant.now().plusSeconds(300))
                .build();

        var response = scheduledTransferService.createScheduledTransfer(oneTimeRequest);

        assertEquals("ONE_TIME", response.getScheduleType());
        assertEquals("ACTIVE", response.getStatus());
        assertNull(response.getOccurrenceCount());
        assertNull(response.getDayOfMonth());
        assertNull(response.getFrequencyDays());
    }

    @Test
    void dailyRecurringScheduledTransfer() {
        CreateScheduledTransferRequest dailyRequest = CreateScheduledTransferRequest.builder()
                .senderAccountId(accountId)
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("10000"))
                .currency("IDR")
                .description("Daily savings")
                .transferType(InitiateTransferRequest.TransactionType.INTERNAL_TRANSFER)
                .scheduleType(ScheduledTransfer.ScheduleType.RECURRING_DAILY)
                .startDate(Instant.now().plusSeconds(300))
                .frequencyDays(1)
                .build();

        var response = scheduledTransferService.createScheduledTransfer(dailyRequest);

        assertEquals("RECURRING_DAILY", response.getScheduleType());
        assertEquals(1, response.getFrequencyDays());
        assertNotNull(response.getNextExecutionDate());
    }
}
