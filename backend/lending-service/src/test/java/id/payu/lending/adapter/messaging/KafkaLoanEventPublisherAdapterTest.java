package id.payu.lending.adapter.messaging;

import id.payu.lending.dto.LoanApprovedEvent;
import id.payu.lending.dto.LoanRejectedEvent;
import id.payu.outbox.service.OutboxService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KafkaLoanEventPublisherAdapterTest {

    private final OutboxService outboxService = mock(OutboxService.class);
    private final KafkaLoanEventPublisherAdapter adapter =
            new KafkaLoanEventPublisherAdapter(outboxService);

    @Test
    void publishesApprovedLoanWithVersionedTopic() {
        UUID loanId = UUID.randomUUID();
        LoanApprovedEvent event = new LoanApprovedEvent(
                loanId,
                UUID.randomUUID(),
                "external-1",
                new BigDecimal("1000000"),
                new BigDecimal("0.14"),
                3,
                new BigDecimal("343382.96"),
                LocalDate.now());

        adapter.publishLoanApproved(event);

        verify(outboxService).createEventFromObject(
                eq("Loan"), eq(loanId.toString()), eq("LoanApproved"), eq(event),
                isNull(), eq("payu.lending.loan-approved.v1"));
    }

    @Test
    void publishesRejectedLoanWithVersionedTopic() {
        UUID loanId = UUID.randomUUID();
        LoanRejectedEvent event = new LoanRejectedEvent(
                loanId,
                UUID.randomUUID(),
                "external-2",
                new BigDecimal("1000000"),
                "Credit score too low");

        adapter.publishLoanRejected(event);

        verify(outboxService).createEventFromObject(
                eq("Loan"), eq(loanId.toString()), eq("LoanRejected"), eq(event),
                isNull(), eq("payu.lending.loan-rejected.v1"));
    }
}
