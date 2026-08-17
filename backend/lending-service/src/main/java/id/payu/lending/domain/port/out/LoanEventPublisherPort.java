package id.payu.lending.domain.port.out;

import id.payu.lending.interfaces.dto.LoanApprovedEvent;
import id.payu.lending.interfaces.dto.LoanRepaymentProcessedEvent;
import id.payu.lending.interfaces.dto.LoanRejectedEvent;

/**
 * Output port for publishing loan-related events.
 * Publishes events to message broker for async processing.
 */
public interface LoanEventPublisherPort {

    /**
     * Publish event when a loan is approved.
     *
     * @param event the loan approved event
     */
    void publishLoanApproved(LoanApprovedEvent event);

    /**
     * Publish event when a loan is rejected.
     *
     * @param event the loan rejected event
     */
    void publishLoanRejected(LoanRejectedEvent event);

    void publishRepaymentProcessed(LoanRepaymentProcessedEvent event);
}
