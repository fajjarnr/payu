package id.payu.loanorigination.service;

import id.payu.loanorigination.domain.DisbursementEvent;
import id.payu.outbox.entity.OutboxEvent;
import id.payu.outbox.repository.OutboxRepository;
import id.payu.outbox.service.OutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisbursementServiceTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private OutboxRepository outboxRepository;

    @Test
    void shouldUseDeterministicReferenceFromProcessId() {
        var service = new DisbursementService(outboxService, outboxRepository);
        when(outboxRepository.findFirstByAggregateTypeAndAggregateIdAndEventType(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        var ref = service.execute("user-1", new BigDecimal("100000"), "PERSONAL_LOAN", 12, "LOAN-11111111-1111-1111-1111-111111111111");

        assertThat(ref).isEqualTo("LOAN-11111111-1111-1111-1111-111111111111");
        ArgumentCaptor<DisbursementEvent> eventCaptor = ArgumentCaptor.forClass(DisbursementEvent.class);
        verify(outboxService).createEventFromObject(
                eq("LoanOrigination"), eq(ref), eq("LoanDisbursed"), eventCaptor.capture(), isNull(), eq("payu.lending.loan-disbursed.v1"));
        assertThat(eventCaptor.getValue().reference()).isEqualTo(ref);
    }

    @Test
    void shouldNotCreateDuplicateEventForDoubleSubmit() {
        var service = new DisbursementService(outboxService, outboxRepository);
        var existing = new OutboxEvent();
        existing.setId(java.util.UUID.randomUUID());
        when(outboxRepository.findFirstByAggregateTypeAndAggregateIdAndEventType(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(existing));

        var ref = service.execute("user-1", new BigDecimal("100000"), "PERSONAL_LOAN", 12, "LOAN-22222222-2222-2222-2222-222222222222");

        assertThat(ref).isEqualTo("LOAN-22222222-2222-2222-2222-222222222222");
        verify(outboxService, never()).createEventFromObject(anyString(), anyString(), anyString(), any(), any(), anyString());
    }
}
