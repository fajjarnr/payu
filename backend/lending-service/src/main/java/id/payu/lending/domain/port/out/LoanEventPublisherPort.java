package id.payu.lending.domain.port.out;

import id.payu.lending.interfaces.dto.LoanApprovedEvent;
import id.payu.lending.interfaces.dto.LoanRepaymentProcessedEvent;
import id.payu.lending.interfaces.dto.LoanRejectedEvent;

/**
 * Output port for publishing loan-related events.
 * Publishes events to message broker for async processing.
 */
public interface LoanEventPublisherPort {

    void publishLoanApproved(LoanApprovedEvent event);

    void publishLoanRejected(LoanRejectedEvent event);

    void publishRepaymentProcessed(LoanRepaymentProcessedEvent event);
}
