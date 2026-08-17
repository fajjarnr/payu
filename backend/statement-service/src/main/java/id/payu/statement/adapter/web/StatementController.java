package id.payu.statement.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.statement.application.service.ReceiptService;
import id.payu.statement.application.service.StatementService;
import id.payu.statement.interfaces.dto.ReceiptGenerationRequest;
import id.payu.statement.interfaces.dto.ReceiptResponse;
import id.payu.statement.interfaces.dto.StatementGenerationRequest;
import id.payu.statement.interfaces.dto.StatementResponse;
import id.payu.statement.adapter.persistence.entity.StatementEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import id.payu.statement.domain.entity.StatementStatus;

/**
 * REST Controller for StatementEntity operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/statements")
@RequiredArgsConstructor
@Tag(name = "StatementEntity", description = "E-StatementEntity generation and management APIs")
@SecurityRequirement(name = "bearerAuth")
public class StatementController extends BaseController {

    private final StatementService statementService;
    private final ReceiptService receiptService;

    @PostMapping("/generate")
    @Operation(summary = "Generate statement", description = "Generate e-statement for a specific month and year")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "StatementEntity generation request accepted",
            content = @Content(schema = @Schema(implementation = StatementResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<StatementResponse>> generateStatement(
            @Valid @RequestBody StatementGenerationRequest request,
            Authentication authentication) {

        // Ensure user can only generate their own statements
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String customerId = jwt.getSubject();
        request.setCustomerId(customerId);

        statementService.generateStatement(request);
        StatementResponse response = StatementResponse.builder()
                .customerId(customerId)
                .status(StatementStatus.GENERATING)
                .build();
        return ResponseEntity.accepted().body(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get statement by ID", description = "Retrieve statement details by statement ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "StatementEntity found",
            content = @Content(schema = @Schema(implementation = StatementResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "StatementEntity not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - cannot access other user's statement")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<StatementResponse>> getStatement(
            @Parameter(description = "StatementEntity ID", required = true) @PathVariable UUID id,
            Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String customerId = jwt.getSubject();

        StatementResponse response = statementService.getStatement(id, customerId);
        return ok(response);
    }

    @GetMapping
    @Operation(summary = "List statements", description = "List all e-statements for the current user with pagination")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statements retrieved successfully",
            content = @Content(schema = @Schema(implementation = StatementResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Page<StatementResponse>>> listStatements(
            @Parameter(description = "Pagination parameters") @PageableDefault(size = 12, sort = "statementPeriod", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String customerId = jwt.getSubject();

        Page<StatementResponse> statements = statementService.listStatements(customerId, pageable);
        return ok(statements, statements);
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest statement", description = "Retrieve the most recent e-statement for the current user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Latest statement found",
            content = @Content(schema = @Schema(implementation = StatementResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No statements found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<StatementResponse>> getLatestStatement(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String customerId = jwt.getSubject();

        return statementService.getLatestStatement(customerId)
            .map(this::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download statement PDF", description = "Download e-statement as PDF file")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PDF downloaded successfully",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "StatementEntity not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - cannot access other user's statement")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<byte[]> downloadStatement(
            @Parameter(description = "StatementEntity ID", required = true) @PathVariable UUID id,
            Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String customerId = jwt.getSubject();

        byte[] pdfBytes = statementService.getStatementPdf(id, customerId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "statement_" + id + ".pdf");

        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfBytes);
    }

    @PostMapping("/{id}/regenerate")
    @Operation(summary = "Regenerate statement", description = "Regenerate an existing e-statement (admin only)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "StatementEntity regeneration request accepted",
            content = @Content(schema = @Schema(implementation = StatementResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "StatementEntity not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StatementResponse>> regenerateStatement(
            @Parameter(description = "StatementEntity ID", required = true) @PathVariable UUID id) {
        statementService.regenerateStatement(id);
        StatementResponse response = StatementResponse.builder()
                .id(id)
                .status(StatementStatus.GENERATING)
                .build();
        return ResponseEntity.accepted().body(ApiResponse.success(response));
    }

    // ==================== Receipt Endpoints (Epic E-19) ====================

    @PostMapping("/receipts/generate")
    @Operation(summary = "Generate transaction receipt", description = "Generate a receipt for a specific transaction")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Receipt generated successfully",
            content = @Content(schema = @Schema(implementation = ReceiptResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction not found")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<ReceiptResponse>> generateReceipt(
            @Valid @RequestBody ReceiptGenerationRequest request,
            Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String customerId = jwt.getSubject();
        request.setCustomerId(customerId);

        ReceiptResponse response = receiptService.generateReceipt(request);
        return ok(response);
    }

    @GetMapping("/receipts/{receiptId}")
    @Operation(summary = "Get receipt by ID", description = "Retrieve receipt details by receipt ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Receipt found",
            content = @Content(schema = @Schema(implementation = ReceiptResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Receipt not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('USER')")
    // BUG-SECURITY-022 FIX: Add ownership validation
    public ResponseEntity<ApiResponse<ReceiptResponse>> getReceipt(
            @Parameter(description = "Receipt ID", required = true) @PathVariable UUID receiptId,
            Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String customerId = jwt.getSubject();
        ReceiptResponse response = receiptService.getReceipt(receiptId, customerId);
        return ok(response);
    }

    @GetMapping("/receipts/transaction/{transactionId}")
    @Operation(summary = "Get receipt by transaction ID", description = "Retrieve receipt for a specific transaction")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Receipt found",
            content = @Content(schema = @Schema(implementation = ReceiptResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Receipt not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('USER')")
    // BUG-SECURITY-022 FIX: Add ownership validation
    public ResponseEntity<ApiResponse<ReceiptResponse>> getReceiptByTransaction(
            @Parameter(description = "Transaction ID", required = true) @PathVariable String transactionId,
            Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String customerId = jwt.getSubject();
        return receiptService.getReceiptByTransactionId(transactionId, customerId)
                .map(this::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/receipts/{receiptId}/download")
    @Operation(summary = "Download receipt PDF", description = "Download transaction receipt as PDF file")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PDF downloaded successfully",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Receipt not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "Receipt has expired")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('USER')")
    // BUG-SECURITY-022 FIX: Add ownership validation
    public ResponseEntity<byte[]> downloadReceipt(
            @Parameter(description = "Receipt ID", required = true) @PathVariable UUID receiptId,
            Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String customerId = jwt.getSubject();
        byte[] pdfBytes = receiptService.generatePdf(receiptId, customerId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "receipt_" + receiptId + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @GetMapping("/receipts/transaction/{transactionId}/download")
    @Operation(summary = "Download receipt PDF by transaction ID", description = "Download transaction receipt as PDF using transaction ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PDF downloaded successfully",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Receipt not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "Receipt has expired")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('USER')")
    // BUG-SECURITY-022 FIX: Add ownership validation
    public ResponseEntity<byte[]> downloadReceiptByTransaction(
            @Parameter(description = "Transaction ID", required = true) @PathVariable String transactionId,
            Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String customerId = jwt.getSubject();
        byte[] pdfBytes = receiptService.generatePdfByTransactionId(transactionId, customerId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "receipt_" + transactionId + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
