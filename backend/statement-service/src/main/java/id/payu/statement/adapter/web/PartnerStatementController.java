package id.payu.statement.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.statement.application.service.StatementService;
import id.payu.statement.interfaces.dto.StatementGenerationRequest;
import id.payu.statement.interfaces.dto.StatementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/**
 * ADR-0019 / ARCH-STATEMENT-001: partner (SNAP-BI project client) statement API.
 * Machine-readable statements for integrators — separate from the end-user PDF API.
 */
@RestController
@RequestMapping("/v1/partner/statements")
@Tag(name = "Partner Statement", description = "SNAP-BI partner statement API (ADR-0019)")
@SecurityRequirement(name = "bearerAuth")
public class PartnerStatementController {

    private static final Logger log = LoggerFactory.getLogger(PartnerStatementController.class);

    private final StatementService statementService;

    public PartnerStatementController(StatementService statementService) {
        this.statementService = statementService;
    }

    private void verifyPartnerAccess(String customerId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return;
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        if (isAdmin) return;

        if (auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            String partnerId = jwt.getClaimAsString("partner_id");
            if (partnerId == null) {
                partnerId = jwt.getSubject();
            }
            if (partnerId != null && !customerId.equals(partnerId) && !customerId.startsWith(partnerId + "-") && !customerId.startsWith("partner-" + partnerId)) {
                throw new org.springframework.security.access.AccessDeniedException("Partner not authorized to access statement for: " + customerId);
            }
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PARTNER', 'ADMIN')")
    @Operation(summary = "Query partner statements (JSON)",
            description = "Returns statements for a customer in a date range (ADR-0019).")
    public ResponseEntity<ApiResponse<List<StatementResponse>>> listStatements(
            @RequestParam String customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        verifyPartnerAccess(customerId);
        log.info("Partner statement query for customerId={}, from={}, to={}", customerId, from, to);

        Page<StatementResponse> page = statementService.listStatements(
                customerId, PageRequest.of(0, 500));
        List<StatementResponse> filtered = page.getContent().stream()
                .filter(s -> from == null || !s.getStatementPeriod().isBefore(from))
                .filter(s -> to == null || !s.getStatementPeriod().isAfter(to))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(filtered));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('PARTNER', 'ADMIN')")
    @Operation(summary = "Request partner statement generation",
            description = "Asks statement-service to generate a statement for a customer period (ADR-0019).")
    public ResponseEntity<ApiResponse<StatementResponse>> generateStatement(
            @Valid @RequestBody StatementGenerationRequest request) {
        log.info("Partner statement generation request: customerId={}", request.getCustomerId());
        statementService.generateStatement(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
