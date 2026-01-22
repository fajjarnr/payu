package id.payu.lending.adapter.web;

import id.payu.lending.application.service.LendingApplicationService;
import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.model.PayLater;
import id.payu.lending.dto.LoanApplicationRequest;
import id.payu.lending.dto.PayLaterLimitRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/lending")
@RequiredArgsConstructor
@Slf4j
public class LendingController {

    private final LendingApplicationService lendingApplicationService;

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
