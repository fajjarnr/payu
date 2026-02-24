package id.payu.partner.adapter.web;

import id.payu.partner.dto.PartnerDTO;
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
import id.payu.security.annotation.Audited.AuditLevel;

import org.springframework.security.access.prepost.PreAuthorize;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

/**
 * REST controller for managing partners.
 * Provides CRUD operations for partner management including key regeneration.
 */
@RestController
@RequestMapping("/partners")
@Tag(name = OpenApiConstants.TAG_PARTNER, description = "Partner management operations")
@PreAuthorize("hasRole('ADMIN')") // BUG-BE-164: Restrict partner management to ADMIN
public class PartnerController extends BaseController {

    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @GetMapping
    @Operation(
        summary = "Get all partners",
        description = "Retrieves a list of all partners registered in the system. Returns basic partner information including client credentials."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = OpenApiConstants.DESCRIPTION_200,
            content = @Content(schema = @Schema(implementation = PartnerListResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = OpenApiConstants.DESCRIPTION_401,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = OpenApiConstants.DESCRIPTION_500,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<?> getAllPartners() {
        return ok(partnerService.getAllPartners());
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get partner by ID",
        description = "Retrieves detailed information about a specific partner identified by their unique ID."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = OpenApiConstants.DESCRIPTION_200,
            content = @Content(schema = @Schema(implementation = PartnerResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = OpenApiConstants.DESCRIPTION_401,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = OpenApiConstants.DESCRIPTION_404,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = OpenApiConstants.DESCRIPTION_500,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<?> getPartnerById(@PathVariable("id") Long id) {
        PartnerDTO partner = partnerService.getPartnerById(id);
        if (partner == null) {
            return notFound("Partner", id);
        }
        return ok(partner);
    }

    @PostMapping
    @Audited(
            operation = Audited.Operation.CREATE,
            entityType = "Partner",
            maskData = true,
            level = AuditLevel.INFO
    )
    @Operation(
        summary = "Create a new partner",
        description = "Creates a new partner with auto-generated client ID and client secret. The partner will be active by default."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = OpenApiConstants.DESCRIPTION_201,
            content = @Content(schema = @Schema(implementation = PartnerResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = OpenApiConstants.DESCRIPTION_400,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = OpenApiConstants.DESCRIPTION_401,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = OpenApiConstants.DESCRIPTION_409,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = OpenApiConstants.DESCRIPTION_500,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<?> createPartner(@Valid @RequestBody PartnerDTO partnerDTO) {
        try {
            PartnerDTO createdPartner = partnerService.createPartner(partnerDTO);
            return created(createdPartner);
        } catch (IllegalArgumentException e) {
            return conflict("PARTNER_EXISTS", e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update partner",
        description = "Updates information for an existing partner. Client credentials cannot be modified through this endpoint."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = OpenApiConstants.DESCRIPTION_200,
            content = @Content(schema = @Schema(implementation = PartnerResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = OpenApiConstants.DESCRIPTION_400,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = OpenApiConstants.DESCRIPTION_401,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = OpenApiConstants.DESCRIPTION_404,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = OpenApiConstants.DESCRIPTION_500,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<?> updatePartner(
            @PathVariable("id") Long id,
            @Valid @RequestBody PartnerDTO partnerDTO) {
        PartnerDTO updatedPartner = partnerService.updatePartner(id, partnerDTO);
        if (updatedPartner == null) {
            return notFound("Partner", id);
        }
        return ok(updatedPartner);
    }

    @PostMapping("/{id}/keys/regenerate")
    @RateLimiter(name = "regenerateKeys") // BUG-BE-165: Rate limiting to prevent abuse
    @Audited(
            operation = Audited.Operation.OTHER,
            entityType = "Partner",
            maskData = true,
            level = AuditLevel.WARN
    )
    @Operation(
        summary = "Regenerate partner API keys",
        description = "Regenerates the client ID and client secret for a partner. This invalidates all existing credentials immediately."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = OpenApiConstants.DESCRIPTION_200,
            content = @Content(schema = @Schema(implementation = PartnerResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = OpenApiConstants.DESCRIPTION_401,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = OpenApiConstants.DESCRIPTION_404,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = OpenApiConstants.DESCRIPTION_500,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<?> regenerateKeys(@PathVariable("id") Long id) {
        PartnerDTO partner = partnerService.regenerateKeys(id);
        if (partner == null) {
            return notFound("Partner", id);
        }
        // BUG-BE-165: Mask the client secret instead of returning it fully
        if (partner.getClientSecret() != null && partner.getClientSecret().length() >= 4) {
            partner.setClientSecret(partner.getClientSecret().substring(0, 4) + "***");
        }
        return ok(partner);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete partner",
        description = "Permanently removes a partner from the system."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204",
            description = OpenApiConstants.DESCRIPTION_204
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = OpenApiConstants.DESCRIPTION_401,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = OpenApiConstants.DESCRIPTION_404,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = OpenApiConstants.DESCRIPTION_500,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<?> deletePartner(@PathVariable("id") Long id) {
        boolean deleted = partnerService.deletePartner(id);
        if (!deleted) {
            return notFound("Partner", id);
        }
        return noContent();
    }
    
    // Fix deletePartner return signature to allow error body
    // Actually I will change the signature of deletePartner method above to ResponseEntity<?> 

    // Inner schema classes
    @Schema(name = "PartnerListResponse", description = "Response containing list of partners")
    private static class PartnerListResponse extends ApiResponse<List<PartnerDTO>> {}

    @Schema(name = "PartnerResponse", description = "Response containing single partner")
    private static class PartnerResponse extends ApiResponse<PartnerDTO> {}
}
