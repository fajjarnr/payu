package id.payu.lending.adapter.messaging;

import id.payu.lending.domain.port.out.LoanEventPublisherPort;
import id.payu.lending.dto.LoanApprovedEvent;
import id.payu.lending.dto.LoanRepaymentProcessedEvent;
import id.payu.lending.dto.LoanRejectedEvent;
import id.payu.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Outbox-backed adapter for publishing loan events.
 * <p>
 * Events are serialised to the outbox_events table within the same DB transaction
 * as the loan approval/rejection, guaranteeing at-least-once delivery to Kafka.
 */
@Component
@RequiredArgsConstructor
public class KafkaLoanEventPublisherAdapter implements LoanEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaLoanEventPublisherAdapter.class);
    private final OutboxService outboxService;
    private static final String AGGREGATE_TYPE = "Loan";

    @Override
    public void publishLoanApproved(LoanApprovedEvent event) {
        log.info("Creating outbox event for loan approved: {}", event.loanId());
        outboxService.createEventFromObject(
                AGGREGATE_TYPE,
                event.loanId().toString(),
                "LoanApproved",
                event,
                null,
                "loan.approved"
        );
    }

    @Override
    public void publishLoanRejected(LoanRejectedEvent event) {
        log.info("Creating outbox event for loan rejected: {}", event.loanId());
        outboxService.createEventFromObject(
                AGGREGATE_TYPE,
                event.loanId().toString(),
                "LoanRejected",
                event,
                null,
                "loan.rejected"
        );
    }

    @Override
    public void publishRepaymentProcessed(LoanRepaymentProcessedEvent event) {
        log.info("Creating outbox event for repayment processed: repaymentId={}, loanId={}",
                event.repaymentId(), event.loanId());
        outboxService.createEventFromObject(
                "LoanRepayment",
                event.repaymentId().toString(),
                "LoanRepaymentProcessed",
                event,
                null,
                "payu.lending.loan-repayment-processed.v1"
        );
    }
}
