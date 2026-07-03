package id.payu.loanorigination.service;

import id.payu.loanorigination.domain.DisbursementEvent;
import id.payu.outbox.service.OutboxService;
import org.kie.api.runtime.process.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component("Disbursement")
public class DisbursementWorkItemHandler implements WorkItemHandler {

    private static final Logger log = LoggerFactory.getLogger(DisbursementWorkItemHandler.class);

    private final OutboxService outboxService;

    public DisbursementWorkItemHandler(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @Override
    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        String userId = (String) workItem.getParameter("userId");
        BigDecimal amount = (BigDecimal) workItem.getParameter("amount");
        String loanType = (String) workItem.getParameter("loanType");
        Integer tenureInt = (Integer) workItem.getParameter("tenureMonths");
        int tenure = tenureInt != null ? tenureInt : 0;

        String reference = UUID.randomUUID().toString();
        log.info("Disbursement: userId={}, amount={}, loanType={}, tenure={}, ref={}",
                userId, amount, loanType, tenure, reference);

        DisbursementEvent event = new DisbursementEvent(userId, amount, loanType, tenure, reference);

        outboxService.createEventFromObject(
                "LoanOrigination",
                reference,
                "LoanDisbursed",
                event,
                null,
                "payu.lending.loan-disbursed.v1");

        log.info("Disbursement event published: ref={}", reference);
        manager.completeWorkItem(workItem.getId(), Map.of("reference", reference));
    }

    @Override
    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        log.warn("Disbursement aborted: workItemId={}", workItem.getId());
        manager.abortWorkItem(workItem.getId());
    }
}
