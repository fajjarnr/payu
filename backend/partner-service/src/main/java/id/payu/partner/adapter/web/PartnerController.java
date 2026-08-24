package id.payu.partner.adapter.web;

import id.payu.partner.interfaces.dto.PartnerDTO;
import id.payu.partner.application.service.PartnerService;
import id.payu.partner.adapter.web.ApiResponse;
import id.payu.partner.adapter.web.BaseController;
import id.payu.partner.adapter.web.OpenApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.AuditLevel;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import java.util.List;
import java.util.Map;
import id.payu.security.annotation.AuditOperation;

/**
 * REST controller for managing partners — ADR-0035 dual-control.
 */
@RestController
@RequestMapping({"/v1/partners", "/partners"})
@Tag(name = OpenApiConstants.TAG_PARTNER, description = "PartnerEntity management operations")
public class PartnerController extends BaseController {

    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    private String resolveUserId(Jwt jwt) {
        if (jwt != null) {
            String sub = jwt.getSubject();
            if (sub != null && !sub.isBlank()) return sub;
            String email = jwt.getClaimAsString("email");
            if (email != null && !email.isBlank()) return email;
            String pref = jwt.getClaimAsString("preferred_username");
            if (pref != null && !pref.isBlank()) return pref;
        }
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) return auth.getName();
        return "unknown";
    }

    @GetMapping
    @Operation(summary = "Get all partners")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @PreAuthorize("hasAnyRole('PARTNER_MAKER','PARTNER_CHECKER','PARTNER_VIEWER','PARTNER_ADMIN','ADMIN')")
    public ResponseEntity<?> getAllPartners() {
        return ok(partnerService.getAllPartners());
    }

    @GetMapping("/me")
    @Operation(summary = "Get my partner profile")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyPartner(@AuthenticationPrincipal Jwt jwt) {
        String email = null;
        if (jwt != null) {
            email = jwt.getClaimAsString("email");
            if (email == null || email.isBlank()) email = jwt.getClaimAsString("preferred_username");
            if (email == null || email.isBlank()) email = jwt.getSubject();
        }
        if ((email == null || email.isBlank()) && org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
                Jwt token = jwtAuth.getToken();
                email = token.getClaimAsString("email");
                if (email == null || email.isBlank()) email = token.getClaimAsString("preferred_username");
                if (email == null || email.isBlank()) email = token.getSubject();
            } else {
                String name = auth.getName();
                if (name != null && name.contains("@")) email = name;
            }
        }
        if (email == null || email.isBlank()) return notFound("PartnerEntity", "me");
        return partnerService.findByEmail(email).<ResponseEntity<?>>map(this::ok).orElse(notFound("PartnerEntity", "me"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get partner by ID")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPartnerById(@PathVariable("id") Long id) {
        PartnerDTO partner = partnerService.getPartnerById(id);
        if (partner == null) return notFound("PartnerEntity", id);
        return ok(partner);
    }

    @PostMapping
    @Audited(operation = AuditOperation.CREATE, entityType = "PartnerEntity", maskData = true, level = AuditLevel.INFO)
    @Operation(summary = "Create a new partner — maker creates PENDING_APPROVAL")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @PreAuthorize("hasAnyRole('PARTNER_MAKER','PARTNER_ADMIN','ADMIN')")
    public ResponseEntity<?> createPartner(@Valid @RequestBody PartnerDTO partnerDTO, @AuthenticationPrincipal Jwt jwt) {
        try {
            String makerId = resolveUserId(jwt);
            PartnerDTO created = partnerService.createPartner(partnerDTO, makerId);
            return created(created);
        } catch (IllegalArgumentException e) {
            return conflict("PARTNER_EXISTS", e.getMessage());
        }
    }

    @PostMapping("/{id}/approve")
    @Audited(operation = AuditOperation.KYC_APPROVE, entityType = "PartnerEntity", maskData = false, level = AuditLevel.WARN)
    @Operation(summary = "Approve partner — checker only, maker≠checker DB-enforced")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @PreAuthorize("hasAnyRole('PARTNER_CHECKER','PARTNER_ADMIN')")
    public ResponseEntity<?> approvePartner(@PathVariable("id") Long id, @AuthenticationPrincipal Jwt jwt,
                                            @RequestHeader(value = "X-Justification", required = false) String justification) {
        String checkerId = resolveUserId(jwt);
        // PARTNER_ADMIN break-glass requires X-Justification
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isAdminOnly = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PARTNER_ADMIN"))
                && auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_PARTNER_CHECKER"));
        if (isAdminOnly && (justification == null || justification.isBlank())) {
            return badRequest("PARTNER_JUSTIFICATION_REQUIRED", "X-Justification header required for PARTNER_ADMIN break-glass");
        }
        PartnerDTO result = partnerService.approvePartner(id, checkerId);
        return ok(result);
    }

    @PostMapping("/{id}/reject")
    @Audited(operation = AuditOperation.KYC_REJECT, entityType = "PartnerEntity", maskData = false, level = AuditLevel.WARN)
    @Operation(summary = "Reject partner — checker only")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @PreAuthorize("hasAnyRole('PARTNER_CHECKER','PARTNER_ADMIN')")
    public ResponseEntity<?> rejectPartner(@PathVariable("id") Long id, @AuthenticationPrincipal Jwt jwt,
                                           @RequestBody(required = false) Map<String, String> body) {
        String checkerId = resolveUserId(jwt);
        String reason = body != null ? body.get("rejection_reason") : null;
        if (reason == null && body != null) reason = body.get("rejectionReason");
        if (reason == null || reason.isBlank()) return badRequest("PARTNER_REJECTION_REASON_REQUIRED", "rejection_reason is required");
        PartnerDTO result = partnerService.rejectPartner(id, checkerId, reason);
        return ok(result);
    }

    @PostMapping("/{id}/resubmit")
    @Audited(operation = AuditOperation.UPDATE, entityType = "PartnerEntity", maskData = true, level = AuditLevel.INFO)
    @Operation(summary = "Resubmit rejected partner — maker only")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @PreAuthorize("hasAnyRole('PARTNER_MAKER','PARTNER_ADMIN','ADMIN')")
    public ResponseEntity<?> resubmitPartner(@PathVariable("id") Long id, @AuthenticationPrincipal Jwt jwt) {
        String makerId = resolveUserId(jwt);
        PartnerDTO result = partnerService.resubmitPartner(id, makerId);
        return ok(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update partner")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePartner(@PathVariable("id") Long id, @Valid @RequestBody PartnerDTO partnerDTO) {
        PartnerDTO updated = partnerService.updatePartner(id, partnerDTO);
        if (updated == null) return notFound("PartnerEntity", id);
        return ok(updated);
    }

    @PostMapping("/{id}/keys/regenerate")
    @RateLimiter(name = "regenerateKeys")
    @Audited(operation = AuditOperation.OTHER, entityType = "PartnerEntity", maskData = true, level = AuditLevel.WARN)
    @Operation(summary = "Regenerate partner API keys")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> regenerateKeys(@PathVariable("id") Long id) {
        PartnerDTO partner = partnerService.regenerateKeys(id);
        if (partner == null) return notFound("PartnerEntity", id);
        if (partner.clientSecret != null && partner.clientSecret.length() >= 4) {
            partner.clientSecret = partner.clientSecret.substring(0, 4) + "***";
        }
        return ok(partner);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete partner — only REJECTED")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @PreAuthorize("hasAnyRole('PARTNER_MAKER','PARTNER_ADMIN','ADMIN')")
    public ResponseEntity<?> deletePartner(@PathVariable("id") Long id) {
        boolean deleted = partnerService.deletePartner(id);
        if (!deleted) return notFound("PartnerEntity", id);
        return noContent();
    }

    @Schema(name = "PartnerListResponse", description = "Response containing list of partners")
    private static class PartnerListResponse extends ApiResponse<List<PartnerDTO>> {}

    @Schema(name = "PartnerResponse", description = "Response containing single partner")
    private static class PartnerResponse extends ApiResponse<PartnerDTO> {}
}
