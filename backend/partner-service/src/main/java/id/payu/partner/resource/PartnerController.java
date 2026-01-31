package id.payu.partner.resource;

import id.payu.partner.dto.PartnerDTO;
import id.payu.partner.service.PartnerService;
import id.payu.partner.web.ApiResponse;
import id.payu.partner.web.BaseController;
import id.payu.partner.web.OpenApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing partners.
 * Provides CRUD operations for partner management including key regeneration.
 */
@RestController
@RequestMapping("/partners")
@Tag(name = OpenApiConstants.TAG_PARTNER, description = "Partner management operations")
@RequiredArgsConstructor
public class PartnerController extends BaseController {

    private final PartnerService partnerService;

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
