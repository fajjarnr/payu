package id.payu.lending.adapter.web;

import id.payu.lending.application.service.LendingApplicationService;
import id.payu.lending.application.service.LoanManagementService;
import id.payu.lending.application.service.PayLaterTransactionService;
import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.model.PayLater;
import id.payu.lending.domain.model.PayLaterTransaction;
import id.payu.lending.domain.model.RepaymentSchedule;
import id.payu.lending.dto.LoanApplicationRequest;
import id.payu.lending.dto.PayLaterLimitRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/lending")
@RequiredArgsConstructor
@Slf4j
public class LendingController {

    private final LendingApplicationService lendingApplicationService;
    private final LoanManagementService loanManagementService;
    private final PayLaterTransactionService payLaterTransactionService;

    @PostMapping("/loans")
    public CompletableFuture<ResponseEntity<Loan>> applyLoan(@Valid @RequestBody LoanApplicationRequest request) {
        log.info("Received loan application request for user: {}", request.userId());
        return lendingApplicationService.applyLoan(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error processing loan application", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    @GetMapping("/loans/{loanId}")
    public ResponseEntity<Loan> getLoan(@PathVariable UUID loanId) {
        log.info("Fetching loan details for loan: {}", loanId);
        return lendingApplicationService.getLoanById(loanId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/loans/{loanId}/repayment-schedule")
    public ResponseEntity<List<RepaymentSchedule>> createRepaymentSchedule(@PathVariable UUID loanId) {
        log.info("Creating repayment schedule for loan: {}", loanId);
        return ResponseEntity.ok(loanManagementService.createRepaymentSchedule(loanId));
    }

    @GetMapping("/loans/{loanId}/repayment-schedule")
    public ResponseEntity<List<RepaymentSchedule>> getRepaymentScheduleByLoanId(@PathVariable UUID loanId) {
        log.info("Fetching repayment schedule for loan: {}", loanId);
        return ResponseEntity.ok(loanManagementService.getRepaymentScheduleByLoanId(loanId));
    }

    @GetMapping("/repayment-schedules/{scheduleId}")
    public ResponseEntity<RepaymentSchedule> getRepaymentSchedule(@PathVariable UUID scheduleId) {
        log.info("Fetching repayment schedule: {}", scheduleId);
        return loanManagementService.getRepaymentSchedule(scheduleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/repayment-schedules/{scheduleId}/pay")
    public ResponseEntity<RepaymentSchedule> processRepayment(
            @PathVariable UUID scheduleId,
            @RequestParam BigDecimal amount) {
        log.info("Processing repayment for schedule: {} with amount: {}", scheduleId, amount);
        return ResponseEntity.ok(loanManagementService.processRepayment(scheduleId, amount));
    }

    @PostMapping("/paylater/activate")
    public ResponseEntity<PayLater> activatePayLater(
            @RequestParam UUID userId,
            @Valid @RequestBody PayLaterLimitRequest request) {
        log.info("Activating PayLater for user: {}", userId);
        return ResponseEntity.ok(lendingApplicationService.activatePayLater(userId, request));
    }

    @GetMapping("/paylater/{userId}")
    public ResponseEntity<PayLater> getPayLater(@PathVariable UUID userId) {
        log.info("Fetching PayLater details for user: {}", userId);
        return lendingApplicationService.getPayLaterByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/paylater/{userId}/purchase")
    public ResponseEntity<PayLaterTransaction> recordPurchase(
            @PathVariable UUID userId,
            @RequestParam String merchantName,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description) {
        log.info("Recording PayLater purchase for user: {} at merchant: {}", userId, merchantName);
        return ResponseEntity.ok(payLaterTransactionService.recordPurchase(userId, merchantName, amount, description));
    }

    @PostMapping("/paylater/{userId}/payment")
    public ResponseEntity<PayLaterTransaction> recordPayment(
            @PathVariable UUID userId,
            @RequestParam BigDecimal amount) {
        log.info("Recording PayLater payment for user: {} with amount: {}", userId, amount);
        return ResponseEntity.ok(payLaterTransactionService.recordPayment(userId, amount));
    }

    @GetMapping("/paylater/{userId}/transactions")
    public ResponseEntity<List<PayLaterTransaction>> getTransactionHistory(@PathVariable UUID userId) {
        log.info("Fetching transaction history for user: {}", userId);
        return ResponseEntity.ok(payLaterTransactionService.getTransactionHistory(userId));
    }

    @PostMapping("/credit-score/calculate")
    public ResponseEntity<id.payu.lending.domain.model.CreditScore> calculateCreditScore(@RequestParam UUID userId) {
        log.info("Calculating credit score for user: {}", userId);
        return ResponseEntity.ok(lendingApplicationService.calculateCreditScore(userId));
    }

    @GetMapping("/credit-score/{userId}")
    public ResponseEntity<id.payu.lending.domain.model.CreditScore> getCreditScore(@PathVariable UUID userId) {
        log.info("Fetching credit score for user: {}", userId);
        return lendingApplicationService.getCreditScoreByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
