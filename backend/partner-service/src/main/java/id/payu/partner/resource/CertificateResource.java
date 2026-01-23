package id.payu.partner.resource;

import id.payu.partner.domain.PartnerCertificate;
import id.payu.partner.dto.CertificateRequest;
import id.payu.partner.dto.PartnerCertificateDTO;
import id.payu.partner.service.CertificateRotationService;
import id.payu.partner.service.CertificateService;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/partners/{partnerId}/certificates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CertificateResource {

    @Inject
    CertificateService certificateService;

    @Inject
    CertificateRotationService certificateRotationService;

    @GET
    public Response getCertificatesByPartner(@PathParam("partnerId") Long partnerId) {
        List<PartnerCertificate> certificates = certificateService.getCertificatesByPartner(partnerId);
        List<PartnerCertificateDTO> dtos = certificates.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return Response.ok(dtos).build();
    }

    @GET
    @Path("/active")
    public Response getActiveCertificate(@PathParam("partnerId") Long partnerId) {
        return certificateService.getActiveCertificate(partnerId)
                .map(cert -> Response.ok(toDTO(cert)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/valid")
    public Response getValidCertificate(@PathParam("partnerId") Long partnerId) {
        return certificateService.getValidCertificate(partnerId)
                .map(cert -> Response.ok(toDTO(cert)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/expiring")
    public Response getExpiringCertificates(@PathParam("partnerId") Long partnerId,
                                             @QueryParam("days") int days) {
        int daysToCheck = days > 0 ? days : 30;
        List<PartnerCertificate> certificates = certificateService.getExpiringCertificates(partnerId, daysToCheck);
        List<PartnerCertificateDTO> dtos = certificates.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return Response.ok(dtos).build();
    }

    @POST
    public Response addCertificate(@PathParam("partnerId") Long partnerId,
                                    @Valid CertificateRequest request) {
        try {
            PartnerCertificate cert = certificateService.addCertificate(
                    partnerId, request.certificatePem, request.privateKeyPem
            );
            return Response.status(Response.Status.CREATED).entity(toDTO(cert)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/generate")
    public Response generateCertificate(@PathParam("partnerId") Long partnerId,
                                        @QueryParam("validityDays") int validityDays) {
        try {
            int days = validityDays > 0 ? validityDays : 365;
            PartnerCertificate cert = certificateService.generateKeyPairAndStore(partnerId, days);
            return Response.status(Response.Status.CREATED).entity(toDTO(cert)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/{certificateId}/validate")
    public Response validateCertificate(@PathParam("certificateId") Long certificateId) {
        boolean isValid = certificateService.validateCertificate(certificateId);
        Map<String, Object> response = new HashMap<>();
        response.put("certificateId", certificateId);
        response.put("valid", isValid);
        return Response.ok(response).build();
    }

    @PUT
    @Path("/{certificateId}/rotate")
    public Response rotateCertificate(@PathParam("partnerId") Long partnerId,
                                       @PathParam("certificateId") Long certificateId,
                                       @QueryParam("validityDays") int validityDays) {
        try {
            int days = validityDays > 0 ? validityDays : 90;
            certificateRotationService.rotateCertificate(certificateId, days);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Certificate rotated successfully");
            response.put("validityDays", days);
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @PUT
    @Path("/rotate-all")
    public Response rotateAllCertificates(@PathParam("partnerId") Long partnerId,
                                           @QueryParam("validityDays") int validityDays) {
        try {
            int days = validityDays > 0 ? validityDays : 90;
            certificateRotationService.rotateCertificateForPartner(partnerId, days);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "All certificates rotated successfully for partner");
            response.put("partnerId", partnerId);
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{certificateId}")
    public Response deleteCertificate(@PathParam("certificateId") Long certificateId) {
        boolean deleted = certificateService.deleteCertificate(certificateId);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @PUT
    @Path("/{certificateId}/deactivate")
    public Response deactivateCertificate(@PathParam("certificateId") Long certificateId) {
        boolean deactivated = certificateService.deactivateCertificate(certificateId);
        if (!deactivated) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok().entity("Certificate deactivated successfully").build();
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
}
