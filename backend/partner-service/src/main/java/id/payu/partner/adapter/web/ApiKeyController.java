package id.payu.partner.adapter.web;

import id.payu.partner.application.service.ApiKeyService;
import id.payu.partner.interfaces.dto.ApiKeyDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import id.payu.security.annotation.Audited;
import id.payu.security.annotation.AuditLevel;

import java.util.Map;
import id.payu.security.annotation.AuditOperation;

/**
 * REST controller for API key lifecycle management.
 * Supports key generation, rotation with grace period, revocation,
 * and rate plan assignment.
 */
@RestController
@RequestMapping("/partners/{partnerId}/api-keys")
@Tag(name = "API Keys", description = "API key management — generation, rotation, revocation")
@PreAuthorize("hasRole('ADMIN')")
public class ApiKeyController extends BaseController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    @Operation(
        summary = "Generate a new API key",
        description = "Creates a new API key for the partner. The full key value is returned only once. "
            + "Max 5 active keys per partner."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", description = "API key created",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Invalid input or max keys reached",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @Audited(operation = AuditOperation.CREATE, entityType = "ApiKey", maskData = true, level = AuditLevel.WARN)
    public ResponseEntity<?> createApiKey(
            @Parameter(description = "PartnerEntity ID") @PathVariable Long partnerId,
            @Valid @RequestBody ApiKeyDTO dto) {
        return created(apiKeyService.createApiKey(partnerId, dto));
    }

    @GetMapping
    @Operation(summary = "List all API keys", description = "Lists all API keys for a partner (without key values).")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<?> listApiKeys(
            @Parameter(description = "PartnerEntity ID") @PathVariable Long partnerId) {
        return ok(apiKeyService.listApiKeys(partnerId));
    }

    @GetMapping("/{keyId}")
    @Operation(summary = "Get API key details", description = "Get details of a specific API key (without key value).")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<?> getApiKey(
            @Parameter(description = "PartnerEntity ID") @PathVariable Long partnerId,
            @Parameter(description = "Key ID") @PathVariable Long keyId) {
        return ok(apiKeyService.getApiKey(partnerId, keyId));
    }

    @PutMapping("/{keyId}")
    @Operation(
        summary = "Update API key settings",
        description = "Update the name, rate plan, or rate limits for an API key."
    )
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @Audited(operation = AuditOperation.UPDATE, entityType = "ApiKey", level = AuditLevel.INFO)
    public ResponseEntity<?> updateApiKey(
            @Parameter(description = "PartnerEntity ID") @PathVariable Long partnerId,
            @Parameter(description = "Key ID") @PathVariable Long keyId,
            @RequestBody ApiKeyDTO dto) {
        return ok(apiKeyService.updateApiKey(partnerId, keyId, dto));
    }

    @PostMapping("/{keyId}/rotate")
    @Operation(
        summary = "Rotate an API key",
        description = "Generates a new key and marks the old one as ROTATED with a 30-day grace period. "
            + "During the grace period, both old and new keys are accepted."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Key rotated — new key returned",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Key not in ACTIVE state",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @Audited(operation = AuditOperation.UPDATE, entityType = "ApiKey", maskData = true, level = AuditLevel.WARN)
    public ResponseEntity<?> rotateApiKey(
            @Parameter(description = "PartnerEntity ID") @PathVariable Long partnerId,
            @Parameter(description = "Key ID") @PathVariable Long keyId) {
        return ok(apiKeyService.rotateApiKey(partnerId, keyId));
    }

    @PostMapping("/{keyId}/revoke")
    @Operation(
        summary = "Revoke an API key",
        description = "Immediately revokes an API key. Revoked keys cannot be re-activated."
    )
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    @Audited(operation = AuditOperation.DELETE, entityType = "ApiKey", level = AuditLevel.WARN)
    public ResponseEntity<?> revokeApiKey(
            @Parameter(description = "PartnerEntity ID") @PathVariable Long partnerId,
            @Parameter(description = "Key ID") @PathVariable Long keyId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        apiKeyService.revokeApiKey(partnerId, keyId, reason);
        return noContent();
    }
}
