package id.payu.loanorigination.adapter.web;

import id.payu.loanorigination.adapter.persistence.LoanOriginationProcessEntity;
import id.payu.loanorigination.domain.LoanOriginationRequest;
import id.payu.loanorigination.service.LoanOriginationProcessService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loan-origination")
public class LoanOriginationController {

    private final LoanOriginationProcessService processService;

    public LoanOriginationController(LoanOriginationProcessService processService) {
        this.processService = processService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> startProcess(
            @RequestBody LoanOriginationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            return ResponseEntity.ok(toResponse(processService.startProcess(request, userId(jwt))));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getProcess(@PathVariable UUID id) {
        return processService.getProcess(id)
                .map(process -> ResponseEntity.ok(toResponse(process)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN', 'BACKOFFICE')")
    public ResponseEntity<Map<String, Object>> approveTask(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean approved,
            @RequestParam(defaultValue = "") String comment,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            return ResponseEntity.ok(toResponse(processService.approve(id, approved, comment, userId(jwt))));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN', 'BACKOFFICE')")
    public ResponseEntity<Map<String, Object>> listProcesses() {
        var result = new LinkedHashMap<String, Object>();
        var ids = processService.listProcessIds();
        result.put("count", ids.size());
        result.put("processes", ids.stream().map(UUID::toString).toList());
        return ResponseEntity.ok(result);
    }

    private static String userId(Jwt jwt) {
        if (jwt == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        String accountId = jwt.getClaimAsString("account_id");
        if (accountId != null && !accountId.isBlank()) {
            return accountId;
        }
        return jwt.getSubject();
    }

    private static Map<String, Object> toResponse(LoanOriginationProcessEntity process) {
        var response = new LinkedHashMap<String, Object>();
        response.put("processId", process.getId());
        response.put("userId", process.getUserId());
        response.put("principalAmount", process.getPrincipalAmount());
        response.put("tenureMonths", process.getTenureMonths());
        response.put("purpose", process.getPurpose());
        response.put("loanType", process.getLoanType());
        response.put("creditScore", process.getCreditScore());
        response.put("status", process.getStatus());
        response.put("approved", process.getApproved());
        response.put("comment", process.getComment());
        response.put("approvedBy", process.getApprovedBy());
        response.put("disbursementReference", process.getDisbursementReference());
        return response;
    }
}
