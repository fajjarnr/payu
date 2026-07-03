package id.payu.loanorigination.service;

import id.payu.loanorigination.domain.DisbursementEvent;
import id.payu.outbox.service.OutboxService;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DisbursementService {

    private static final Logger log = LoggerFactory.getLogger(DisbursementService.class);

    private final OutboxService outboxService;

    public DisbursementService(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    public String execute(String userId, BigDecimal amount, String loanType, int tenureMonths) {
        var ref = UUID.randomUUID().toString();
        log.info("Disbursement: userId={}, amount={}, type={}, tenure={}, ref={}", userId, amount, loanType, tenureMonths, ref);
        var event = new DisbursementEvent(userId, amount, loanType, tenureMonths, ref);
        outboxService.createEventFromObject(
                "LoanOrigination", ref, "LoanDisbursed", event, null,
                "payu.lending.loan-disbursed.v1");
        return ref;
    }
}
