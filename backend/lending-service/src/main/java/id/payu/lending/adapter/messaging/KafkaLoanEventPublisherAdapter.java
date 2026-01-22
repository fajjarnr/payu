package id.payu.lending.adapter.messaging;

import id.payu.lending.domain.port.out.LoanEventPublisherPort;
import id.payu.lending.dto.LoanApprovedEvent;
import id.payu.lending.dto.LoanRejectedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaLoanEventPublisherAdapter implements LoanEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaLoanEventPublisherAdapter.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String LOAN_APPROVED_TOPIC = "loan.approved";
    private static final String LOAN_REJECTED_TOPIC = "loan.rejected";

    @Override
    public void publishLoanApproved(LoanApprovedEvent event) {
        try {
            log.info("Publishing loan approved event for loan: {}", event.loanId());
            kafkaTemplate.send(LOAN_APPROVED_TOPIC, event.loanId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish loan approved event", e);
        }
    }

    @Override
    public void publishLoanRejected(LoanRejectedEvent event) {
        try {
            log.info("Publishing loan rejected event for loan: {}", event.loanId());
            kafkaTemplate.send(LOAN_REJECTED_TOPIC, event.loanId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish loan rejected event", e);
        }
    }
}
