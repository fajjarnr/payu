package id.payu.statement.api;

import id.payu.statement.service.StatementService;
import id.payu.statement.service.dto.StatementGenerationRequest;
import id.payu.statement.service.dto.StatementResponse;
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
public class StatementController {

    private final StatementService statementService;

    /**
     * Generate statement for a specific month
     * POST /api/v1/statements/generate
     */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> generateStatement(
            @Valid @RequestBody StatementGenerationRequest request,
            Authentication authentication) {

        // Ensure user can only generate their own statements
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getSubject());
        request.setUserId(userId);

        statementService.generateStatement(request);
        return ResponseEntity.accepted().build();
    }

    /**
     * Get statement by ID
     * GET /api/v1/statements/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<StatementResponse> getStatement(
            @PathVariable UUID id,
            Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getSubject());

        StatementResponse response = statementService.getStatement(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * List all statements for current user
     * GET /api/v1/statements
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<StatementResponse>> listStatements(
            @PageableDefault(size = 12, sort = "statementPeriod", direction = Sort.Direction.DESCENDING) Pageable pageable,
            Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getSubject());

        Page<StatementResponse> statements = statementService.listStatements(userId, pageable);
        return ResponseEntity.ok(statements);
    }

    /**
     * Get latest statement for current user
     * GET /api/v1/statements/latest
     */
    @GetMapping("/latest")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<StatementResponse> getLatestStatement(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getSubject());

        return statementService.getLatestStatement(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Download statement PDF
     * GET /api/v1/statements/{id}/download
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<byte[]> downloadStatement(
            @PathVariable UUID id,
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

    /**
     * Regenerate statement (admin only)
     * POST /api/v1/statements/{id}/regenerate
     */
    @PostMapping("/{id}/regenerate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> regenerateStatement(@PathVariable UUID id) {
        statementService.regenerateStatement(id);
        return ResponseEntity.accepted().build();
    }
}
