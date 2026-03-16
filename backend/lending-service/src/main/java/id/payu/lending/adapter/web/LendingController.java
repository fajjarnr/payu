package id.payu.lending.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.lending.application.security.LendingSecurityService;
import id.payu.lending.application.service.InstallmentService;
import id.payu.lending.application.service.LendingApplicationService;
import id.payu.lending.application.service.LoanManagementService;
import id.payu.lending.application.service.PayLaterTransactionService;
import id.payu.lending.domain.model.Loan;
import id.payu.lending.domain.model.PayLater;
import id.payu.lending.domain.model.PayLaterTransaction;
import id.payu.lending.domain.model.RepaymentSchedule;
import id.payu.lending.dto.LoanApplicationCommand;
import id.payu.lending.dto.LoanApplicationRequest;
import id.payu.lending.dto.LoanPreApprovalResponse;
import id.payu.lending.dto.PayLaterLimitRequest;
import id.payu.lending.dto.InstallmentCheckoutRequest;
import id.payu.lending.dto.InstallmentCheckoutResponse;
import id.payu.lending.dto.TenorOptionResponse;
import id.payu.lending.dto.TenorOptionsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import id.payu.commons.idempotency.Idempotent;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.Audited.AuditLevel;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/lending")
@RequiredArgsConstructor
@Tag(name = "Lending", description = "Personal Loan, PayLater, and Credit Scoring APIs")
@SecurityRequirement(name = "bearerAuth")
public class LendingController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(LendingController.class);

    private final LendingApplicationService lendingApplicationService;
    private final LoanManagementService loanManagementService;
    private final PayLaterTransactionService payLaterTransactionService;
    private final id.payu.lending.application.service.LoanPreApprovalService preApprovalService;
    private final LendingSecurityService lendingSecurityService;
    private final InstallmentService installmentService;

    @PostMapping("/loans")
    @PreAuthorize("isAuthenticated()")
    @Idempotent(required = true)
    @Audited(
            operation = id.payu.security.annotation.Audited.Operation.OTHER,
            entityType = "Loan",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Operation(summary = "Apply for a loan", description = "Submit a new loan application")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Loan application submitted successfully",
            content = @Content(schema = @Schema(implementation = Loan.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - cannot apply loan for another user")
    public CompletableFuture<ResponseEntity<ApiResponse<Loan>>> applyLoan(
            @Valid @RequestBody LoanApplicationCommand command,
            java.security.Principal principal) {
        // Extract userId from JWT (authentication.name) instead of request body
        UUID authenticatedUserId = UUID.fromString(principal.getName());
        log.info("Received loan application request for authenticated user: {}", authenticatedUserId);

        // Create a new request with the authenticated user's ID
        LoanApplicationRequest securedRequest = new LoanApplicationRequest(
                authenticatedUserId,
                command.externalId(),
                command.loanType(),
                command.principalAmount(),
                command.tenureMonths(),
                command.purpose()
        );

        return lendingApplicationService.applyLoan(securedRequest)
                .orTimeout(30, TimeUnit.SECONDS)
                .thenApply(loan -> {
                    URI location = ServletUriComponentsBuilder
                            .fromCurrentRequest()
                            .path("/{loanId}")
                            .buildAndExpand(loan.getId())
                            .toUri();
                    return created(loan, location.toString());
                })
                .exceptionally(ex -> {
                    log.error("Error processing loan application", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error("LENDING_001", "Error processing loan application"));
                });
    }

    @GetMapping("/loans/{loanId}")
    @PreAuthorize("isAuthenticated() and @lendingSecurityService.isLoanOwner(#loanId, authentication.principal.userId)")
    @Operation(summary = "Get loan by ID", description = "Retrieve loan details by loan ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Loan found",
            content = @Content(schema = @Schema(implementation = Loan.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Loan not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - loan access denied")
    public ResponseEntity<ApiResponse<Loan>> getLoan(
            @Parameter(description = "Loan ID", required = true) @PathVariable UUID loanId) {
        log.info("Fetching loan details for loan: {}", loanId);
        return lendingApplicationService.getLoanById(loanId)
                .map(this::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/loans/{loanId}/repayment-schedule")
    @PreAuthorize("isAuthenticated() and @lendingSecurityService.isLoanOwner(#loanId, authentication.principal.userId)")
    @Operation(summary = "Create repayment schedule", description = "Generate repayment schedule for a loan")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Repayment schedule created successfully",
            content = @Content(schema = @Schema(implementation = RepaymentSchedule.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Loan not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<List<RepaymentSchedule>>> createRepaymentSchedule(
            @Parameter(description = "Loan ID", required = true) @PathVariable UUID loanId) {
        log.info("Creating repayment schedule for loan: {}", loanId);
        List<RepaymentSchedule> schedules = loanManagementService.createRepaymentSchedule(loanId);
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/lending/loans/{loanId}/repayment-schedule")
                .buildAndExpand(loanId)
                .toUri();
        return created(schedules, location.toString());
    }

    @GetMapping("/loans/{loanId}/repayment-schedule")
    @PreAuthorize("isAuthenticated() and @lendingSecurityService.isLoanOwner(#loanId, authentication.principal.userId)")
    @Operation(summary = "Get repayment schedule by loan", description = "Retrieve repayment schedule for a specific loan")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Repayment schedule retrieved successfully",
            content = @Content(schema = @Schema(implementation = RepaymentSchedule.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<List<RepaymentSchedule>>> getRepaymentScheduleByLoanId(
            @Parameter(description = "Loan ID", required = true) @PathVariable UUID loanId) {
        log.info("Fetching repayment schedule for loan: {}", loanId);
        return ok(loanManagementService.getRepaymentScheduleByLoanId(loanId));
    }

    @GetMapping("/repayment-schedules/{scheduleId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get repayment schedule by ID", description = "Retrieve a specific repayment schedule")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Repayment schedule found",
            content = @Content(schema = @Schema(implementation = RepaymentSchedule.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Repayment schedule not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<RepaymentSchedule>> getRepaymentSchedule(
            @Parameter(description = "Schedule ID", required = true) @PathVariable UUID scheduleId) {
        log.info("Fetching repayment schedule: {}", scheduleId);
        return loanManagementService.getRepaymentSchedule(scheduleId)
                .map(this::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/repayment-schedules/{scheduleId}/pay")
    @PreAuthorize("isAuthenticated()")
    @Idempotent(required = true)
    @Operation(summary = "Process repayment", description = "Make a repayment for a specific schedule")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Repayment processed successfully",
            content = @Content(schema = @Schema(implementation = RepaymentSchedule.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid amount")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Schedule not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<RepaymentSchedule>> processRepayment(
            @Parameter(description = "Schedule ID", required = true) @PathVariable UUID scheduleId,
            // BUG-BE-085: Changed from @RequestParam to @RequestBody — financial amounts must not be in URL
            @RequestBody java.util.Map<String, BigDecimal> body) {
        BigDecimal amount = body.get("amount");
        log.info("Processing repayment for schedule: {} with amount: {}", scheduleId, amount);
        return ok(loanManagementService.processRepayment(scheduleId, amount));
    }

    @PostMapping("/paylater/activate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Activate PayLater", description = "Activate PayLater service for a user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "PayLater activated successfully",
            content = @Content(schema = @Schema(implementation = PayLater.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<PayLater>> activatePayLater(
            @Parameter(description = "User ID", required = true) @RequestParam UUID userId,
            @Valid @RequestBody PayLaterLimitRequest request) {
        log.info("Activating PayLater for user: {}", userId);
        PayLater payLater = lendingApplicationService.activatePayLater(userId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/lending/paylater/{userId}")
                .buildAndExpand(userId)
                .toUri();

        return created(payLater, location.toString());
    }

    @GetMapping("/paylater/{userId}")
    @PreAuthorize("isAuthenticated() and @lendingSecurityService.isPaylaterOwner(#userId, authentication.principal.userId)")
    @Operation(summary = "Get PayLater details", description = "Retrieve PayLater account details for a user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PayLater details found",
            content = @Content(schema = @Schema(implementation = PayLater.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PayLater not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - PayLater access denied")
    public ResponseEntity<ApiResponse<PayLater>> getPayLater(
            @Parameter(description = "User ID", required = true) @PathVariable UUID userId) {
        log.info("Fetching PayLater details for user: {}", userId);
        return lendingApplicationService.getPayLaterByUserId(userId)
                .map(this::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/paylater/{userId}/purchase")
    @PreAuthorize("isAuthenticated() and @lendingSecurityService.isPaylaterOwner(#userId, authentication.principal.userId)")
    @Idempotent(required = true)
    @Audited(
            operation = id.payu.security.annotation.Audited.Operation.OTHER,
            entityType = "PayLaterTransaction",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Operation(summary = "Record PayLater purchase", description = "Record a purchase transaction using PayLater")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Purchase recorded successfully",
            content = @Content(schema = @Schema(implementation = PayLaterTransaction.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or insufficient limit")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PayLater not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<PayLaterTransaction>> recordPurchase(
            @Parameter(description = "User ID", required = true) @PathVariable UUID userId,
            @Parameter(description = "Merchant name", required = true) @RequestParam String merchantName,
            @Parameter(description = "Purchase amount", required = true) @RequestParam BigDecimal amount,
            @Parameter(description = "Transaction description") @RequestParam(required = false) String description) {
        log.info("Recording PayLater purchase for user: {} at merchant: {}", userId, merchantName);
        PayLaterTransaction transaction = payLaterTransactionService.recordPurchase(userId, merchantName, amount, description);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/lending/paylater/{userId}/transactions/{transactionId}")
                .buildAndExpand(userId, transaction.getId())
                .toUri();

        return created(transaction, location.toString());
    }

    @PostMapping("/paylater/{userId}/payment")
    @PreAuthorize("isAuthenticated() and @lendingSecurityService.isPaylaterOwner(#userId, authentication.principal.userId)")
    @Idempotent(required = true)
    @Operation(summary = "Record PayLater payment", description = "Record a payment transaction for PayLater")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment recorded successfully",
            content = @Content(schema = @Schema(implementation = PayLaterTransaction.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid amount")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PayLater not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<PayLaterTransaction>> recordPayment(
            @Parameter(description = "User ID", required = true) @PathVariable UUID userId,
            @Parameter(description = "Payment amount", required = true) @RequestParam BigDecimal amount) {
        log.info("Recording PayLater payment for user: {} with amount: {}", userId, amount);
        PayLaterTransaction transaction = payLaterTransactionService.recordPayment(userId, amount);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/lending/paylater/{userId}/transactions/{transactionId}")
                .buildAndExpand(userId, transaction.getId())
                .toUri();

        return created(transaction, location.toString());
    }

    @GetMapping("/paylater/{userId}/transactions")
    @PreAuthorize("isAuthenticated() and @lendingSecurityService.isPaylaterOwner(#userId, authentication.principal.userId)")
    @Operation(summary = "Get transaction history", description = "Retrieve PayLater transaction history for a user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully",
            content = @Content(schema = @Schema(implementation = PayLaterTransaction.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - PayLater access denied")
    public ResponseEntity<ApiResponse<List<PayLaterTransaction>>> getTransactionHistory(
            @Parameter(description = "User ID", required = true) @PathVariable UUID userId) {
        log.info("Fetching transaction history for user: {}", userId);
        return ok(payLaterTransactionService.getTransactionHistory(userId));
    }

    @PostMapping("/credit-score/calculate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Calculate credit score", description = "Calculate credit score for a user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Credit score calculated successfully",
            content = @Content(schema = @Schema(implementation = id.payu.lending.domain.model.CreditScore.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<id.payu.lending.domain.model.CreditScore>> calculateCreditScore(
            @Parameter(description = "User ID", required = true) @RequestParam UUID userId) {
        log.info("Calculating credit score for user: {}", userId);
        return ok(lendingApplicationService.calculateCreditScore(userId));
    }

    @GetMapping("/credit-score/{userId}")
    @PreAuthorize("isAuthenticated() and @lendingSecurityService.isCreditScoreOwner(#userId, authentication.principal.userId)")
    @Operation(summary = "Get credit score", description = "Retrieve credit score for a user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Credit score found",
            content = @Content(schema = @Schema(implementation = id.payu.lending.domain.model.CreditScore.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Credit score not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - credit score access denied")
    public ResponseEntity<ApiResponse<id.payu.lending.domain.model.CreditScore>> getCreditScore(
            @Parameter(description = "User ID", required = true) @PathVariable UUID userId) {
        log.info("Fetching credit score for user: {}", userId);
        return lendingApplicationService.getCreditScoreByUserId(userId)
                .map(this::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/pre-approval/check")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check loan pre-approval", description = "Check if user is pre-approved for a loan")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Pre-approval check completed",
            content = @Content(schema = @Schema(implementation = LoanPreApprovalResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<LoanPreApprovalResponse>> checkPreApproval(
            @Valid @RequestBody id.payu.lending.dto.LoanPreApprovalRequest request) {
        log.info("Checking loan pre-approval for user: {}", request.userId());
        LoanPreApprovalResponse response = preApprovalService.checkPreApproval(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/lending/pre-approval/{preApprovalId}")
                .buildAndExpand(response.preApprovalId())
                .toUri();

        return created(response, location.toString());
    }

    @GetMapping("/pre-approval/{preApprovalId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get pre-approval by ID", description = "Retrieve pre-approval details by ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pre-approval found",
            content = @Content(schema = @Schema(implementation = id.payu.lending.domain.model.LoanPreApproval.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Pre-approval not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<id.payu.lending.domain.model.LoanPreApproval>> getPreApproval(
            @Parameter(description = "Pre-approval ID", required = true) @PathVariable UUID preApprovalId) {
        log.info("Fetching pre-approval by ID: {}", preApprovalId);
        return preApprovalService.getPreApprovalById(preApprovalId)
                .map(this::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pre-approval/user/{userId}/active")
    @PreAuthorize("isAuthenticated() and @lendingSecurityService.isPreApprovalOwner(#userId, authentication.principal.userId)")
    @Operation(summary = "Get active pre-approval", description = "Retrieve active pre-approval for a user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active pre-approval found",
            content = @Content(schema = @Schema(implementation = id.payu.lending.domain.model.LoanPreApproval.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No active pre-approval found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - pre-approval access denied")
    public ResponseEntity<ApiResponse<id.payu.lending.domain.model.LoanPreApproval>> getActivePreApproval(
            @Parameter(description = "User ID", required = true) @PathVariable UUID userId) {
        log.info("Fetching active pre-approval for user: {}", userId);
        return preApprovalService.getActivePreApprovalByUserId(userId)
                .map(this::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ═══════════════════════════════════════════════════════
    //  Installment / PayLater Checkout (GAP-012)
    // ═══════════════════════════════════════════════════════

    @PostMapping("/installments/tenor-options")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get installment tenor options",
            description = "Returns available tenor options (3x/6x/12x) with simulated monthly payment")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenor options returned")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient credit or invalid request")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<List<TenorOptionResponse>>> getTenorOptions(
            @Valid @RequestBody TenorOptionsRequest request) {
        log.info("Tenor options requested: userId={}, amount={}", request.userId(), request.amount());
        List<TenorOptionResponse> options = installmentService
                .getTenorOptions(request.userId(), request.amount())
                .stream()
                .map(TenorOptionResponse::from)
                .toList();
        return ok(options);
    }

    @PostMapping("/installments/checkout")
    @PreAuthorize("isAuthenticated()")
    @Idempotent(required = true)
    @Audited(
            operation = id.payu.security.annotation.Audited.Operation.TRANSFER,
            entityType = "InstallmentCheckout",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Operation(summary = "Create installment checkout",
            description = "Convert a purchase into an installment loan via PayLater credit")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Checkout created and disbursed",
            content = @Content(schema = @Schema(implementation = InstallmentCheckoutResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient credit or invalid request")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<ApiResponse<InstallmentCheckoutResponse>> checkout(
            @Valid @RequestBody InstallmentCheckoutRequest request) {
        log.info("Installment checkout: userId={}, partner={}, amount={}, tenor={}x",
                request.userId(), request.partnerId(), request.amount(), request.tenor());
        id.payu.lending.domain.model.InstallmentCheckout result = installmentService.checkout(
                request.userId(), request.partnerId(), request.externalOrderId(),
                request.amount(), request.tenor());
        return created(InstallmentCheckoutResponse.from(result),
                "/api/v1/lending/installments/" + result.getId());
    }

    @GetMapping("/installments/{checkoutId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get installment checkout by ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Checkout found",
            content = @Content(schema = @Schema(implementation = InstallmentCheckoutResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Checkout not found")
    public ResponseEntity<ApiResponse<InstallmentCheckoutResponse>> getCheckout(
            @Parameter(description = "Checkout UUID") @PathVariable UUID checkoutId) {
        id.payu.lending.domain.model.InstallmentCheckout checkout = installmentService.getCheckout(checkoutId);
        return ok(InstallmentCheckoutResponse.from(checkout));
    }

    @GetMapping("/installments/user/{userId}")
    @PreAuthorize("isAuthenticated() and @lendingSecurityService.isPaylaterOwner(#userId, authentication.principal.userId)")
    @Operation(summary = "Get installment checkouts for a user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Checkouts returned")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    public ResponseEntity<ApiResponse<List<InstallmentCheckoutResponse>>> getCheckoutsByUser(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        List<InstallmentCheckoutResponse> checkouts = installmentService.getCheckoutsByUser(userId)
                .stream()
                .map(InstallmentCheckoutResponse::from)
                .toList();
        return ok(checkouts);
    }
}
