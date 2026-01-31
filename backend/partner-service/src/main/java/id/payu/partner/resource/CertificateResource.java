package id.payu.partner.resource;

import id.payu.partner.domain.PartnerCertificate;
import id.payu.partner.dto.CertificateRequest;
import id.payu.partner.dto.PartnerCertificateDTO;
import id.payu.partner.service.CertificateRotationService;
import id.payu.partner.service.CertificateService;
import id.payu.partner.web.ApiResponse;
import id.payu.partner.web.BaseController;
import id.payu.partner.web.OpenApiConstants;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST resource for managing partner certificates.
 * Provides operations for certificate lifecycle management including rotation and validation.
 *
 * <p>This resource handles:</p>
 * <ul>
 *   <li>Retrieving certificates for a partner</li>
 *   <li>Getting active/valid/expiring certificates</li>
 *   <li>Adding new certificates</li>
 *   <li>Generating certificate key pairs</li>
 *   <li>Validating certificates</li>
 *   <li>Rotating certificates</li>
 *   <li>Deactivating and deleting certificates</li>
 * </ul>
 */
@Path("/partners/{partnerId}/certificates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = OpenApiConstants.TAG_CERTIFICATE, description = "Certificate management operations")
@ApplicationScoped
public class CertificateResource extends BaseController {

    @Inject
    CertificateService certificateService;

    @Inject
    CertificateRotationService certificateRotationService;

    /**
     * Retrieves all certificates for a specific partner.
     *
     * @param partnerId the unique identifier of the partner
     * @return List of all certificates for the partner
     */
    @GET
    @Operation(
        summary = "Get all certificates for a partner",
        description = "Retrieves all certificates associated with a specific partner, " +
            "including both active and inactive certificates."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = OpenApiConstants.DESCRIPTION_200,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = CertificateListResponse.class)
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
    public Response getCertificatesByPartner(
        @PathParam("partnerId") @Schema(description = "Partner ID", example = "1") Long partnerId) {
        List<PartnerCertificate> certificates = certificateService.getCertificatesByPartner(partnerId);
        List<PartnerCertificateDTO> dtos = certificates.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ok(dtos);
    }

    /**
     * Retrieves the active certificate for a partner.
     *
     * @param partnerId the unique identifier of the partner
     * @return The currently active certificate
     */
    @GET
    @Path("/active")
    @Operation(
        summary = "Get active certificate",
        description = "Retrieves the currently active certificate for a partner. " +
            "Returns 404 if no active certificate exists."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = OpenApiConstants.DESCRIPTION_200,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = CertificateResponse.class)
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
    public Response getActiveCertificate(
        @PathParam("partnerId") @Schema(description = "Partner ID", example = "1") Long partnerId) {
        return certificateService.getActiveCertificate(partnerId)
                .map(cert -> ok(toDTO(cert)))
                .orElse(notFound("Active certificate not found for partner", partnerId));
    }

    /**
     * Retrieves a valid certificate for a partner.
     * Returns a certificate that is both active and within its validity period.
     *
     * @param partnerId the unique identifier of the partner
     * @return A valid certificate if one exists
     */
    @GET
    @Path("/valid")
    @Operation(
        summary = "Get valid certificate",
        description = "Retrieves a valid certificate for a partner. " +
            "A valid certificate must be active and within its validity period. " +
            "Returns 404 if no valid certificate exists."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = OpenApiConstants.DESCRIPTION_200,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = CertificateResponse.class)
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
    public Response getValidCertificate(
        @PathParam("partnerId") @Schema(description = "Partner ID", example = "1") Long partnerId) {
        return certificateService.getValidCertificate(partnerId)
                .map(cert -> ok(toDTO(cert)))
                .orElse(notFound("Valid certificate not found for partner", partnerId));
    }

    /**
     * Retrieves certificates that will expire within a specified number of days.
     *
     * @param partnerId the unique identifier of the partner
     * @param days      the number of days to look ahead for expiration (default: 30)
     * @return List of certificates expiring within the specified period
     */
    @GET
    @Path("/expiring")
    @Operation(
        summary = "Get expiring certificates",
        description = "Retrieves certificates that will expire within the specified number of days. " +
            "Useful for proactive certificate rotation before expiration."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = OpenApiConstants.DESCRIPTION_200,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = CertificateListResponse.class)
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
    public Response getExpiringCertificates(
        @PathParam("partnerId") @Schema(description = "Partner ID", example = "1") Long partnerId,
        @QueryParam("days") @Parameter(
            description = "Number of days to look ahead for expiration",
            example = "30"
        ) @Min(value = 1, message = "Days must be at least 1") int days) {
        int daysToCheck = days > 0 ? days : 30;
        List<PartnerCertificate> certificates = certificateService.getExpiringCertificates(partnerId, daysToCheck);
        List<PartnerCertificateDTO> dtos = certificates.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ok(dtos);
    }

    /**
     * Adds a new certificate for a partner.
     *
     * @param partnerId the unique identifier of the partner
     * @param request   the certificate PEM and private key
     * @return Created certificate details
     */
    @POST
    @Operation(
        summary = "Add a new certificate",
        description = "Adds a new certificate with private key for a partner. " +
            "The certificate and private key should be in PEM format. " +
            "The certificate will be stored securely and can be activated for use."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "201",
            description = OpenApiConstants.DESCRIPTION_201,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = CertificateResponse.class)
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
    public Response addCertificate(
        @PathParam("partnerId") @Schema(description = "Partner ID", example = "1") Long partnerId,
        @Valid
        @Schema(description = "Certificate data in PEM format", implementation = CertificateRequest.class)
        CertificateRequest request) {
        try {
            PartnerCertificate cert = certificateService.addCertificate(
                    partnerId, request.certificatePem, request.privateKeyPem
            );
            return created(toDTO(cert));
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return badRequest("CERTIFICATE_INVALID", e.getMessage());
        }
    }

    /**
     * Generates a new key pair and certificate for a partner.
     *
     * @param partnerId   the unique identifier of the partner
     * @param validityDays the validity period in days (default: 365)
     * @return Generated certificate details
     */
    @POST
    @Path("/generate")
    @Operation(
        summary = "Generate a new certificate",
        description = "Generates a new RSA key pair and self-signed certificate for a partner. " +
            "The certificate validity period can be specified. " +
            "Default validity is 365 days. " +
            "The generated certificate will be stored but not activated by default."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "201",
            description = OpenApiConstants.DESCRIPTION_201,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = CertificateResponse.class)
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
    public Response generateCertificate(
        @PathParam("partnerId") @Schema(description = "Partner ID", example = "1") Long partnerId,
        @QueryParam("validityDays") @Parameter(
            description = "Certificate validity period in days",
            example = "365"
        ) @Min(value = 1, message = "Validity days must be at least 1") int validityDays) {
        try {
            int days = validityDays > 0 ? validityDays : 365;
            PartnerCertificate cert = certificateService.generateKeyPairAndStore(partnerId, days);
            return created(toDTO(cert));
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalError(e.getMessage());
        }
    }

    /**
     * Validates a certificate by its ID.
     *
     * @param certificateId the unique identifier of the certificate
     * @return Validation result
     */
    @GET
    @Path("/{certificateId}/validate")
    @Operation(
        summary = "Validate a certificate",
        description = "Validates whether a certificate is currently valid. " +
            "Checks if the certificate is active and within its validity period."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = OpenApiConstants.DESCRIPTION_200,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = CertificateValidationResponse.class)
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
    public Response validateCertificate(
        @PathParam("certificateId") @Schema(description = "Certificate ID", example = "1") Long certificateId) {
        boolean isValid = certificateService.validateCertificate(certificateId);
        Map<String, Object> response = new HashMap<>();
        response.put("certificateId", certificateId);
        response.put("valid", isValid);
        return ok(response);
    }

    /**
     * Rotates a specific certificate.
     * Creates a new certificate and deactivates the old one.
     *
     * @param partnerId    the unique identifier of the partner
     * @param certificateId the unique identifier of the certificate to rotate
     * @param validityDays  the validity period for the new certificate (default: 90)
     * @return Rotation confirmation
     */
    @PUT
    @Path("/{certificateId}/rotate")
    @Operation(
        summary = "Rotate a certificate",
        description = "Rotates a specific certificate by generating a new key pair and certificate, " +
            "then deactivating the old certificate. " +
            "Default validity for the new certificate is 90 days."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = OpenApiConstants.DESCRIPTION_200,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = RotationResponse.class)
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
    public Response rotateCertificate(
        @PathParam("partnerId") @Schema(description = "Partner ID", example = "1") Long partnerId,
        @PathParam("certificateId") @Schema(description = "Certificate ID to rotate", example = "1") Long certificateId,
        @QueryParam("validityDays") @Parameter(
            description = "Validity period for new certificate in days",
            example = "90"
        ) @Min(value = 1, message = "Validity days must be at least 1") int validityDays) {
        try {
            int days = validityDays > 0 ? validityDays : 90;
            certificateRotationService.rotateCertificate(certificateId, days);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Certificate rotated successfully");
            response.put("validityDays", days);
            return ok(response);
        } catch (IllegalArgumentException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            return internalError(e.getMessage());
        }
    }

    /**
     * Rotates all certificates for a partner.
     *
     * @param partnerId    the unique identifier of the partner
     * @param validityDays  the validity period for new certificates (default: 90)
     * @return Rotation confirmation
     */
    @PUT
    @Path("/rotate-all")
    @Operation(
        summary = "Rotate all certificates for a partner",
        description = "Rotates all certificates associated with a partner. " +
            "Generates new key pairs and certificates for all existing certificates, " +
            "then deactivates the old ones. " +
            "Default validity for new certificates is 90 days."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = OpenApiConstants.DESCRIPTION_200,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = RotationAllResponse.class)
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
            responseCode = "500",
            description = OpenApiConstants.DESCRIPTION_500,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public Response rotateAllCertificates(
        @PathParam("partnerId") @Schema(description = "Partner ID", example = "1") Long partnerId,
        @QueryParam("validityDays") @Parameter(
            description = "Validity period for new certificates in days",
            example = "90"
        ) @Min(value = 1, message = "Validity days must be at least 1") int validityDays) {
        try {
            int days = validityDays > 0 ? validityDays : 90;
            certificateRotationService.rotateCertificateForPartner(partnerId, days);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "All certificates rotated successfully for partner");
            response.put("partnerId", partnerId);
            return ok(response);
        } catch (Exception e) {
            return internalError(e.getMessage());
        }
    }

    /**
     * Deletes a certificate.
     *
     * @param certificateId the unique identifier of the certificate to delete
     * @return No content on success
     */
    @DELETE
    @Path("/{certificateId}")
    @Operation(
        summary = "Delete a certificate",
        description = "Permanently removes a certificate from the system. " +
            "This action cannot be undone."
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
    public Response deleteCertificate(
        @PathParam("certificateId") @Schema(description = "Certificate ID", example = "1") Long certificateId) {
        boolean deleted = certificateService.deleteCertificate(certificateId);
        if (!deleted) {
            return notFound("Certificate", certificateId);
        }
        return noContent();
    }

    /**
     * Deactivates a certificate without deleting it.
     *
     * @param certificateId the unique identifier of the certificate to deactivate
     * @return Success message
     */
    @PUT
    @Path("/{certificateId}/deactivate")
    @Operation(
        summary = "Deactivate a certificate",
        description = "Deactivates a certificate without deleting it. " +
            "The certificate will no longer be used for signing operations."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = OpenApiConstants.DESCRIPTION_200,
            content = @Content(schema = @Schema(implementation = DeactivateResponse.class))
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
    public Response deactivateCertificate(
        @PathParam("certificateId") @Schema(description = "Certificate ID", example = "1") Long certificateId) {
        boolean deactivated = certificateService.deactivateCertificate(certificateId);
        if (!deactivated) {
            return notFound("Certificate", certificateId);
        }
        Map<String, String> response = new HashMap<>();
        response.put("message", "Certificate deactivated successfully");
        return ok(response);
    }

    private PartnerCertificateDTO toDTO(PartnerCertificate cert) {
        return new PartnerCertificateDTO(
                cert.id,
                cert.partner != null ? cert.partner.id : null,
                cert.publicKeyFingerprint,
                cert.certificateType,
                cert.keyAlgorithm,
                cert.keySize,
                cert.validFrom,
                cert.validTo,
                cert.active,
                cert.issuer,
                cert.subject,
                cert.createdAt,
                cert.updatedAt
        );
    }

    // Schema classes for OpenAPI documentation
    @Schema(name = "CertificateListResponse", description = "Response containing list of certificates")
    private static class CertificateListResponse extends ApiResponse<List<PartnerCertificateDTO>> {}

    @Schema(name = "CertificateResponse", description = "Response containing single certificate")
    private static class CertificateResponse extends ApiResponse<PartnerCertificateDTO> {}

    @Schema(name = "CertificateValidationResponse", description = "Response containing certificate validation result")
    private static class CertificateValidationResponse extends ApiResponse<Map<String, Object>> {}

    @Schema(name = "RotationResponse", description = "Response containing rotation confirmation")
    private static class RotationResponse extends ApiResponse<Map<String, Object>> {}

    @Schema(name = "RotationAllResponse", description = "Response containing rotation all confirmation")
    private static class RotationAllResponse extends ApiResponse<Map<String, Object>> {}

    @Schema(name = "DeactivateResponse", description = "Response containing deactivation confirmation")
    private static class DeactivateResponse extends ApiResponse<Map<String, String>> {}
}
