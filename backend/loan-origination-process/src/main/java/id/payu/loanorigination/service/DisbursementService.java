package id.payu.loanorigination.service;

import id.payu.loanorigination.domain.DisbursementEvent;
import id.payu.outbox.repository.OutboxRepository;
import id.payu.outbox.service.OutboxService;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DisbursementService {

    private static final Logger log = LoggerFactory.getLogger(DisbursementService.class);

    private static final String AGGREGATE_TYPE = "LoanOrigination";
    private static final String EVENT_TYPE = "LoanDisbursed";

    private final OutboxService outboxService;
    private final OutboxRepository outboxRepository;

    public DisbursementService(OutboxService outboxService, OutboxRepository outboxRepository) {
        this.outboxService = outboxService;
        this.outboxRepository = outboxRepository;
    }

    /**
     * Publishes a loan disbursement event with a deterministic reference derived
     * from the loan process id (ARCH-LOAN-001 / ADR-0022). Replaying the same
     * process id never creates a second event: the outbox row is the dedup guard.
     */
    public String execute(String userId, BigDecimal amount, String loanType, int tenureMonths, String reference) {
        if (outboxRepository.findFirstByAggregateTypeAndAggregateIdAndEventType(
                AGGREGATE_TYPE, reference, EVENT_TYPE).isPresent()) {
            log.info("Disbursement already published for reference {}, skipping duplicate", reference);
            return reference;
        }
        log.info("Disbursement: userId={}, amount={}, type={}, tenure={}, ref={}", userId, amount, loanType, tenureMonths, reference);
        var event = new DisbursementEvent(userId, amount, loanType, tenureMonths, reference);
        outboxService.createEventFromObject(
                AGGREGATE_TYPE, reference, EVENT_TYPE, event, null,
                "payu.lending.loan-disbursed.v1");
        return reference;
    }
}
