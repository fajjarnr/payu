package id.payu.investment.domain.port.out;

import id.payu.investment.dto.InvestmentEvent;

/**
 * Output port for publishing investment-related events.
 * Publishes events to message broker for async processing.
 */
public interface InvestmentEventPublisherPort {

    /**
     * Publish event when investment is created.
     *
     * @param event the investment created event
     */
    void publishInvestmentCreated(InvestmentEvent event);

    /**
     * Publish event when investment is completed/matured.
     *
     * @param event the investment completed event
     */
    void publishInvestmentCompleted(InvestmentEvent event);

    /**
     * Publish event when investment fails.
     *
     * @param event the investment failed event
     */
    void publishInvestmentFailed(InvestmentEvent event);
}
