package id.payu.statement.api;

import id.payu.api.common.response.ApiResponse;
import id.payu.statement.service.StatementService;
import id.payu.statement.service.dto.StatementGenerationRequest;
import id.payu.statement.service.dto.StatementResponse;
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

/**
 * REST Controller for Statement operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/statements")
@RequiredArgsConstructor
@Tag(name = "Statement", description = "E-Statement generation and management APIs")
@SecurityRequirement(name = "bearerAuth")
public class StatementController extends BaseController {

    private final StatementService statementService;

    @PostMapping("/generate")
    @Operation(summary = "Generate statement", description = "Generate e-statement for a specific month and year")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Statement generation request accepted",
            content = @Content(schema = @Schema(implementation = StatementResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<StatementResponse>> generateStatement(
            @Valid @RequestBody StatementGenerationRequest request,
            Authentication authentication) {

        // Ensure user can only generate their own statements
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getSubject());
        request.setUserId(userId);

        StatementResponse response = statementService.generateStatement(request);
        return ResponseEntity.accepted().body(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get statement by ID", description = "Retrieve statement details by statement ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statement found",
            content = @Content(schema = @Schema(implementation = StatementResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Statement not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - cannot access other user's statement")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<StatementResponse>> getStatement(
            @Parameter(description = "Statement ID", required = true) @PathVariable UUID id,
            Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getSubject());

        StatementResponse response = statementService.getStatement(id, userId);
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
        UUID userId = UUID.fromString(jwt.getSubject());

        Page<StatementResponse> statements = statementService.listStatements(userId, pageable);
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
        UUID userId = UUID.fromString(jwt.getSubject());

        return statementService.getLatestStatement(userId)
            .map(this::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download statement PDF", description = "Download e-statement as PDF file")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PDF downloaded successfully",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Statement not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - cannot access other user's statement")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<byte[]> downloadStatement(
            @Parameter(description = "Statement ID", required = true) @PathVariable UUID id,
            Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getSubject());

        byte[] pdfBytes = statementService.getStatementPdf(id, userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "statement_" + id + ".pdf");

        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfBytes);
    }

    @PostMapping("/{id}/regenerate")
    @Operation(summary = "Regenerate statement", description = "Regenerate an existing e-statement (admin only)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Statement regeneration request accepted",
            content = @Content(schema = @Schema(implementation = StatementResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Statement not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StatementResponse>> regenerateStatement(
            @Parameter(description = "Statement ID", required = true) @PathVariable UUID id) {
        StatementResponse response = statementService.regenerateStatement(id);
        return ResponseEntity.accepted().body(ApiResponse.success(response));
    }
}
