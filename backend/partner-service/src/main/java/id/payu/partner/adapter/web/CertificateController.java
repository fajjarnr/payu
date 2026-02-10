package id.payu.partner.adapter.web;

import id.payu.partner.domain.PartnerCertificate;
import id.payu.partner.dto.CertificateRequest;
import id.payu.partner.dto.PartnerCertificateDTO;
import id.payu.partner.application.service.CertificateRotationService;
import id.payu.partner.application.service.CertificateService;
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
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for managing partner certificates.
 * Provides operations for certificate lifecycle management including rotation and validation.
 */
@RestController
@RequestMapping("/partners/{partnerId}/certificates")
@Tag(name = OpenApiConstants.TAG_CERTIFICATE, description = "Certificate management operations")
@RequiredArgsConstructor
public class CertificateController extends BaseController {

    private final CertificateService certificateService;
    private final CertificateRotationService certificateRotationService;

    @GetMapping
    @Operation(summary = "Get all certificates for a partner")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = OpenApiConstants.DESCRIPTION_200, content = @Content(schema = @Schema(implementation = CertificateListResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = OpenApiConstants.DESCRIPTION_401, content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = OpenApiConstants.DESCRIPTION_500, content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<ApiResponse<List<PartnerCertificateDTO>>> getCertificatesByPartner(
        @PathVariable("partnerId") Long partnerId) {
        List<PartnerCertificate> certificates = certificateService.getCertificatesByPartner(partnerId);
        List<PartnerCertificateDTO> dtos = certificates.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ok(dtos);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active certificate")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<ApiResponse<PartnerCertificateDTO>> getActiveCertificate(
        @PathVariable("partnerId") Long partnerId) {
        PartnerCertificate cert = certificateService.getActiveCertificate(partnerId).orElse(null);
        if (cert == null) {
            return notFound("Active certificate not found for partner", partnerId);
        }
        return ok(toDTO(cert));
    }

    @GetMapping("/valid")
    @Operation(summary = "Get valid certificate")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<ApiResponse<PartnerCertificateDTO>> getValidCertificate(
        @PathVariable("partnerId") Long partnerId) {
        PartnerCertificate cert = certificateService.getValidCertificate(partnerId).orElse(null);
        if (cert == null) {
            return notFound("Valid certificate not found for partner", partnerId);
        }
        return ok(toDTO(cert));
    }

    @GetMapping("/expiring")
    @Operation(summary = "Get expiring certificates")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<ApiResponse<List<PartnerCertificateDTO>>> getExpiringCertificates(
        @PathVariable("partnerId") Long partnerId,
        @RequestParam(value = "days", defaultValue = "30") @Min(1) int days) {
        int daysToCheck = days > 0 ? days : 30;
        List<PartnerCertificate> certificates = certificateService.getExpiringCertificates(partnerId, daysToCheck);
        List<PartnerCertificateDTO> dtos = certificates.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ok(dtos);
    }

    @PostMapping
    @Operation(summary = "Add a new certificate")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<ApiResponse<PartnerCertificateDTO>> addCertificate(
        @PathVariable("partnerId") Long partnerId,
        @Valid @RequestBody CertificateRequest request) {
        try {
            PartnerCertificate cert = certificateService.addCertificate(
                    partnerId, request.certificatePem, request.privateKeyPem
            );
            return created(toDTO(cert));
        } catch (IllegalArgumentException e) {
            return (ResponseEntity) notFound(e.getMessage());
        } catch (Exception e) {
            return (ResponseEntity) badRequest("CERTIFICATE_INVALID", e.getMessage());
        }
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate a new certificate")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<ApiResponse<PartnerCertificateDTO>> generateCertificate(
        @PathVariable("partnerId") Long partnerId,
        @RequestParam(value = "validityDays", defaultValue = "365") @Min(1) int validityDays) {
        try {
            int days = validityDays > 0 ? validityDays : 365;
            PartnerCertificate cert = certificateService.generateKeyPairAndStore(partnerId, days);
            return created(toDTO(cert));
        } catch (IllegalArgumentException e) {
            return (ResponseEntity) notFound(e.getMessage());
        } catch (Exception e) {
            return (ResponseEntity) internalError(e.getMessage());
        }
    }

    @GetMapping("/{certificateId}/validate")
    @Operation(summary = "Validate a certificate")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateCertificate(
        @PathVariable("certificateId") Long certificateId) {
        boolean isValid = certificateService.validateCertificate(certificateId);
        Map<String, Object> response = new HashMap<>();
        response.put("certificateId", certificateId);
        response.put("valid", isValid);
        return ok(response);
    }

    @PutMapping("/{certificateId}/rotate")
    @Operation(summary = "Rotate a certificate")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<ApiResponse<Map<String, Object>>> rotateCertificate(
        @PathVariable("partnerId") Long partnerId,
        @PathVariable("certificateId") Long certificateId,
        @RequestParam(value = "validityDays", defaultValue = "90") @Min(1) int validityDays) {
        try {
            int days = validityDays > 0 ? validityDays : 90;
            certificateRotationService.rotateCertificate(certificateId, days);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Certificate rotated successfully");
            response.put("validityDays", days);
            return ok(response);
        } catch (IllegalArgumentException e) {
            return (ResponseEntity) notFound(e.getMessage());
        } catch (Exception e) {
            return (ResponseEntity) internalError(e.getMessage());
        }
    }

    @PutMapping("/rotate-all")
    @Operation(summary = "Rotate all certificates for a partner")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<ApiResponse<Map<String, Object>>> rotateAllCertificates(
        @PathVariable("partnerId") Long partnerId,
        @RequestParam(value = "validityDays", defaultValue = "90") @Min(1) int validityDays) {
        try {
            int days = validityDays > 0 ? validityDays : 90;
            certificateRotationService.rotateCertificateForPartner(partnerId, days);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "All certificates rotated successfully for partner");
            response.put("partnerId", partnerId);
            return ok(response);
        } catch (Exception e) {
            return (ResponseEntity) internalError(e.getMessage());
        }
    }

    @DeleteMapping("/{certificateId}")
    @Operation(summary = "Delete a certificate")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<Void> deleteCertificate(
        @PathVariable("certificateId") Long certificateId) {
        boolean deleted = certificateService.deleteCertificate(certificateId);
        if (!deleted) {
            return (ResponseEntity) notFound("Certificate", certificateId);
        }
        return noContent();
    }

    @PutMapping("/{certificateId}/deactivate")
    @Operation(summary = "Deactivate a certificate")
    @SecurityRequirement(name = OpenApiConstants.SECURITY_SCHEME_BEARER)
    public ResponseEntity<ApiResponse<Map<String, String>>> deactivateCertificate(
        @PathVariable("certificateId") Long certificateId) {
        boolean deactivated = certificateService.deactivateCertificate(certificateId);
        if (!deactivated) {
            return (ResponseEntity) notFound("Certificate", certificateId);
        }
        Map<String, String> response = new HashMap<>();
        response.put("message", "Certificate deactivated successfully");
        return ok(response);
    }

    private PartnerCertificateDTO toDTO(PartnerCertificate cert) {
        return new PartnerCertificateDTO(
                cert.getId(),
                cert.getPartner() != null ? cert.getPartner().getId() : null,
                cert.getPublicKeyFingerprint(),
                cert.getCertificateType(),
                cert.getKeyAlgorithm(),
                cert.getKeySize(),
                cert.getValidFrom(),
                cert.getValidTo(),
                cert.isActive(),
                cert.getIssuer(),
                cert.getSubject(),
                cert.getCreatedAt(),
                cert.getUpdatedAt()
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
