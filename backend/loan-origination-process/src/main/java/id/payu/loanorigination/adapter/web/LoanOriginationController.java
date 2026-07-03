package id.payu.loanorigination.adapter.web;

import id.payu.loanorigination.domain.LoanOriginationRequest;
import id.payu.loanorigination.service.CreditScoringService;
import id.payu.loanorigination.service.DisbursementService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/loan-origination")
public class LoanOriginationController {

    private static final Logger log = LoggerFactory.getLogger(LoanOriginationController.class);

    private final CreditScoringService creditScoring;
    private final DisbursementService disbursement;
    private final ConcurrentHashMap<String, Map<String, Object>> processStore = new ConcurrentHashMap<>();

    public LoanOriginationController(CreditScoringService creditScoring, DisbursementService disbursement) {
        this.creditScoring = creditScoring;
        this.disbursement = disbursement;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> startProcess(@RequestBody LoanOriginationRequest request) {
        var id = java.util.UUID.randomUUID().toString();
        log.info("Starting process {}: userId={}, amount={}", id, request.userId(), request.principalAmount());

        BigDecimal score = creditScoring.evaluate(request.principalAmount(), request.tenureMonths());

        Map<String, Object> state = new HashMap<>();
        state.put("processId", id);
        state.put("userId", request.userId());
        state.put("principalAmount", request.principalAmount());
        state.put("tenureMonths", request.tenureMonths());
        state.put("loanType", request.loanType());
        state.put("purpose", request.purpose());
        state.put("creditScore", score);
        state.put("approved", null);

        if (score.compareTo(new BigDecimal("600")) < 0) {
            state.put("status", "REJECTED_LOW_SCORE");
            state.put("message", "Credit score below minimum threshold (600)");
        } else {
            state.put("status", "PENDING_APPROVAL");
            state.put("message", "Awaiting loan officer approval");
        }

        processStore.put(id, state);
        return ResponseEntity.ok(state);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProcess(@PathVariable String id) {
        var state = processStore.get(id);
        if (state == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(state);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveTask(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean approved,
            @RequestParam(defaultValue = "") String comment) {

        var state = processStore.get(id);
        if (state == null) return ResponseEntity.notFound().build();

        log.info("Approval for {}: approved={}, comment={}", id, approved, comment);

        if (approved) {
            String userId = (String) state.get("userId");
            BigDecimal amount = (BigDecimal) state.get("principalAmount");
            String loanType = (String) state.get("loanType");
            Integer tenure = (Integer) state.get("tenureMonths");

            disbursement.execute(userId, amount, loanType != null ? loanType : "PERSONAL_LOAN", tenure != null ? tenure : 0);

            state.put("status", "APPROVED");
            state.put("approved", true);
            state.put("comment", comment);
        } else {
            state.put("status", "REJECTED");
            state.put("approved", false);
            state.put("comment", comment);
        }

        return ResponseEntity.ok(state);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listProcesses() {
        var result = new HashMap<String, Object>();
        result.put("count", processStore.size());
        result.put("processes", processStore.keySet().stream().sorted().toList());
        return ResponseEntity.ok(result);
    }
}
