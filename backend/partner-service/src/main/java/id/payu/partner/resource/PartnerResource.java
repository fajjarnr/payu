package id.payu.partner.resource;

import id.payu.partner.dto.PartnerDTO;
import id.payu.partner.service.PartnerService;
import id.payu.partner.web.ApiResponse;
import id.payu.partner.web.BaseController;
import id.payu.partner.web.FieldError;
import id.payu.partner.web.OpenApiConstants;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/**
 * REST resource for managing partners.
 * Provides CRUD operations for partner management including key regeneration.
 *
 * <p>This resource handles:</p>
 * <ul>
 *   <li>Listing all partners</li>
 *   <li>Retrieving individual partner details</li>
 *   <li>Creating new partners</li>
 *   <li>Updating existing partner information</li>
 *   <li>Regenerating API keys for partners</li>
 *   <li>Deleting partners</li>
 * </ul>
 */
@Path("/partners")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = OpenApiConstants.TAG_PARTNER, description = "Partner management operations")
@ApplicationScoped
public class PartnerResource extends BaseController {

    @Inject
    PartnerService partnerService;

    /**
     * Retrieves all partners in the system.
     *
     * @return List of all partners
     */
    @GET
    @Operation(
        summary = "Get all partners",
        description = "Retrieves a list of all partners registered in the system. " +
            "Returns basic partner information including client credentials."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = OpenApiConstants.DESCRIPTION_200,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = PartnerListResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "401",
            description = OpenApiConstants.DESCRIPTION_401,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @APIResponse(
            responseCode = "500",
            description = OpenApiConstants.DESCRIPTION_500,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public Response getAllPartners() {
        List<PartnerDTO> partners = partnerService.getAllPartners();
        return ok(partners);
    }

    /**
     * Retrieves a specific partner by ID.
     *
     * @param id the unique identifier of the partner
     * @return Partner details if found
     */
    @GET
    @Path("/{id}")
    @Operation(
        summary = "Get partner by ID",
        description = "Retrieves detailed information about a specific partner identified by their unique ID."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = OpenApiConstants.DESCRIPTION_200,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = PartnerResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "401",
            description = OpenApiConstants.DESCRIPTION_401,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @APIResponse(
            responseCode = "404",
            description = OpenApiConstants.DESCRIPTION_404,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @APIResponse(
            responseCode = "500",
            description = OpenApiConstants.DESCRIPTION_500,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public Response getPartnerById(@PathParam("id") @Schema(description = "Partner ID", example = "1") Long id) {
        PartnerDTO partner = partnerService.getPartnerById(id);
        if (partner == null) {
            return notFound("Partner", id);
        }
        return ok(partner);
    }

    /**
     * Creates a new partner in the system.
     *
     * @param partnerDTO the partner data to create
     * @return Created partner with generated credentials
     */
    @POST
    @Operation(
        summary = "Create a new partner",
        description = "Creates a new partner with auto-generated client ID and client secret. " +
            "The partner will be active by default. Credentials should be stored securely."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "201",
            description = OpenApiConstants.DESCRIPTION_201,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = PartnerResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = OpenApiConstants.DESCRIPTION_400,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @APIResponse(
            responseCode = "401",
            description = OpenApiConstants.DESCRIPTION_401,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @APIResponse(
            responseCode = "409",
            description = OpenApiConstants.DESCRIPTION_409,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @APIResponse(
            responseCode = "500",
            description = OpenApiConstants.DESCRIPTION_500,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public Response createPartner(
        @Valid
        @Schema(description = "Partner data to create", implementation = PartnerDTO.class)
        PartnerDTO partnerDTO) {
        try {
            PartnerDTO createdPartner = partnerService.createPartner(partnerDTO);
            return created(createdPartner);
        } catch (IllegalArgumentException e) {
            return conflict("PARTNER_EXISTS", e.getMessage());
        }
    }

    /**
     * Updates an existing partner.
     *
     * @param id         the unique identifier of the partner to update
     * @param partnerDTO the updated partner data
     * @return Updated partner details
     */
    @PUT
    @Path("/{id}")
    @Operation(
        summary = "Update partner",
        description = "Updates information for an existing partner. " +
            "Client credentials (clientId/clientSecret) cannot be modified through this endpoint."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = OpenApiConstants.DESCRIPTION_200,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = PartnerResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "400",
            description = OpenApiConstants.DESCRIPTION_400,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @APIResponse(
            responseCode = "401",
            description = OpenApiConstants.DESCRIPTION_401,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @APIResponse(
            responseCode = "404",
            description = OpenApiConstants.DESCRIPTION_404,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @APIResponse(
            responseCode = "500",
            description = OpenApiConstants.DESCRIPTION_500,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public Response updatePartner(
        @PathParam("id") @Schema(description = "Partner ID", example = "1") Long id,
        @Valid
        @Schema(description = "Updated partner data", implementation = PartnerDTO.class)
        PartnerDTO partnerDTO) {
        PartnerDTO updatedPartner = partnerService.updatePartner(id, partnerDTO);
        if (updatedPartner == null) {
            return notFound("Partner", id);
        }
        return ok(updatedPartner);
    }

    /**
     * Regenerates API keys for a partner.
     * This invalidates existing keys and generates new ones.
     *
     * @param id the unique identifier of the partner
     * @return Updated partner with new credentials
     */
    @POST
    @Path("/{id}/keys/regenerate")
    @Operation(
        summary = "Regenerate partner API keys",
        description = "Regenerates the client ID and client secret for a partner. " +
            "IMPORTANT: This invalidates all existing credentials immediately. " +
            "The new credentials should be stored securely as they will not be retrievable again."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = OpenApiConstants.DESCRIPTION_200,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = PartnerResponse.class)
            )
        ),
        @APIResponse(
            responseCode = "401",
            description = OpenApiConstants.DESCRIPTION_401,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @APIResponse(
            responseCode = "404",
            description = OpenApiConstants.DESCRIPTION_404,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @APIResponse(
            responseCode = "500",
            description = OpenApiConstants.DESCRIPTION_500,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public Response regenerateKeys(
        @PathParam("id") @Schema(description = "Partner ID", example = "1") Long id) {
        PartnerDTO partner = partnerService.regenerateKeys(id);
        if (partner == null) {
            return notFound("Partner", id);
        }
        return ok(partner);
    }

    /**
     * Deletes a partner from the system.
     *
     * @param id the unique identifier of the partner to delete
     * @return No content on success
     */
    @DELETE
    @Path("/{id}")
    @Operation(
        summary = "Delete partner",
        description = "Permanently removes a partner from the system. " +
            "This action cannot be undone. All associated certificates will also be deleted."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "204",
            description = OpenApiConstants.DESCRIPTION_204
        ),
        @APIResponse(
            responseCode = "401",
            description = OpenApiConstants.DESCRIPTION_401,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @APIResponse(
            responseCode = "404",
            description = OpenApiConstants.DESCRIPTION_404,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @APIResponse(
            responseCode = "500",
            description = OpenApiConstants.DESCRIPTION_500,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public Response deletePartner(
        @PathParam("id") @Schema(description = "Partner ID", example = "1") Long id) {
        boolean deleted = partnerService.deletePartner(id);
        if (!deleted) {
            return notFound("Partner", id);
        }
        return noContent();
    }

    // Schema classes for OpenAPI documentation
    @Schema(name = "PartnerListResponse", description = "Response containing list of partners")
    private static class PartnerListResponse extends ApiResponse<List<PartnerDTO>> {}

    @Schema(name = "PartnerResponse", description = "Response containing single partner")
    private static class PartnerResponse extends ApiResponse<PartnerDTO> {}
}
