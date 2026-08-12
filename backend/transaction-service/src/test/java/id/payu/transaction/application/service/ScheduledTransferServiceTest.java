package id.payu.transaction.application.service;

import id.payu.transaction.adapter.persistence.entity.ScheduledTransferEntity;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommand;
import id.payu.transaction.application.cqrs.command.InitiateTransferCommandResult;
import id.payu.transaction.domain.model.ScheduleType;
import id.payu.transaction.domain.model.ScheduledStatus;
import id.payu.transaction.domain.port.in.TransactionUseCase;
import id.payu.transaction.domain.model.TransactionType;
import id.payu.transaction.domain.port.out.ScheduledTransferPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledTransferServiceTest {

    @Mock
    private ScheduledTransferPersistencePort persistencePort;
    @Mock
    private TransactionUseCase transactionUseCase;

    private ScheduledTransferService service;

    @BeforeEach
    void setUp() {
        service = new ScheduledTransferService(persistencePort, transactionUseCase,
                Clock.fixed(Instant.parse("2026-08-12T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void processDueScheduledTransfer_usesDeterministicIdempotencyKeyPerExecution() {
        UUID id = UUID.randomUUID();
        ScheduledTransferEntity transfer = ScheduledTransferEntity.builder()
                .id(id)
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("100000"))
                .currency("IDR")
                .description("rent")
                .transferType(TransactionType.INTERNAL_TRANSFER)
                .scheduleType(ScheduleType.RECURRING_MONTHLY)
                .nextExecutionDate(Instant.parse("2026-08-01T00:00:00Z"))
                .executedCount(0)
                .status(ScheduledStatus.ACTIVE)
                .build();

        when(transactionUseCase.initiateTransfer(any(InitiateTransferCommand.class)))
                .thenReturn(new InitiateTransferCommandResult(
                        UUID.randomUUID(), "TXN-001", "COMPLETED", BigDecimal.ZERO, null));
        when(persistencePort.save(any(ScheduledTransferEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.processDueScheduledTransfer(transfer);

        ArgumentCaptor<InitiateTransferCommand> captor = ArgumentCaptor.forClass(InitiateTransferCommand.class);
        verify(transactionUseCase).initiateTransfer(captor.capture());
        assertThat(captor.getValue().idempotencyKey()).isEqualTo("SCH-" + id + "-0");
    }

    @Test
    void processDueScheduledTransfer_advancesKeyAfterExecution() {
        UUID id = UUID.randomUUID();
        ScheduledTransferEntity transfer = ScheduledTransferEntity.builder()
                .id(id)
                .senderAccountId(UUID.randomUUID())
                .recipientAccountNumber("1234567890")
                .amount(new BigDecimal("100000"))
                .currency("IDR")
                .description("rent")
                .transferType(TransactionType.INTERNAL_TRANSFER)
                .scheduleType(ScheduleType.RECURRING_MONTHLY)
                .nextExecutionDate(Instant.parse("2026-08-01T00:00:00Z"))
                .executedCount(0)
                .status(ScheduledStatus.ACTIVE)
                .build();

        when(transactionUseCase.initiateTransfer(any(InitiateTransferCommand.class)))
                .thenReturn(new InitiateTransferCommandResult(
                        UUID.randomUUID(), "TXN-001", "COMPLETED", BigDecimal.ZERO, null));
        when(persistencePort.save(any(ScheduledTransferEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.processDueScheduledTransfer(transfer);

        verify(persistencePort).save(any(ScheduledTransferEntity.class));
        assertThat(transfer.getExecutedCount()).isEqualTo(1);
    }
}
