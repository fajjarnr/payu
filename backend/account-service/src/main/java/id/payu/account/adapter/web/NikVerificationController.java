package id.payu.account.adapter.web;

import id.payu.account.domain.port.in.VerifyNikUseCase;
import id.payu.account.dto.VerifyNikRequest;
import id.payu.account.dto.VerifyNikResponse;
import id.payu.security.annotation.Audited;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import id.payu.security.annotation.AuditOperation;

/**
 * REST controller for NIK verification operations.
 * Provides endpoint for validating NIK through Dukcapil integration.
 *
 * <p>Endpoint documentation:
 * <ul>
 *   <li>POST /api/v1/accounts/verify-nik - Verify NIK with Dukcapil</li>
 * </ul>
 *
 * <p>Security: Requires authentication with SCOPE_account:verify
 *
 * <p>Response codes:
 * <ul>
 *   <li>200 - Verification completed successfully</li>
 *   <li>400 - Invalid request format or validation error</li>
 *   <li>401 - Authentication required</li>
 *   <li>403 - Insufficient permissions</li>
 *   <li>503 - Dukcapil service unavailable</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "NIK Verification", description = "Indonesia NIK (National ID) verification via Dukcapil integration")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class NikVerificationController {

    private final VerifyNikUseCase verifyNikUseCase;

    /**
     * Verify NIK via Dukcapil simulator.
     *
     * <p>Validates NIK against Dukcapil database and returns verification status
     * with citizen data. Requires authentication and supports rate limiting.
     *
     * @param request the verification request containing NIK and personal data
     * @return CompletableFuture with verification result
     */
    @PostMapping(value = "/verify-nik", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SCOPE_account:verify')")
    @Audited(
        operation = AuditOperation.READ,
        entityType = "NikVerification",
        maskData = true
    )
    @Operation(summary = "Verify NIK with Dukcapil", description = "Validate Indonesia NIK against government database")
    @ApiResponse(responseCode = "200", description = "NIK verified successfully",
            content = @Content(schema = @Schema(implementation = VerifyNikResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request format or validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized - valid JWT token required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    @ApiResponse(responseCode = "503", description = "Dukcapil service unavailable")
    public CompletableFuture<ResponseEntity<VerifyNikResponse>> verifyNik(
        @Valid @RequestBody VerifyNikRequest request
    ) {
        log.info("Received NIK verification request for: ****{}",
            request.nik() != null && request.nik().length() >= 4
                ? request.nik().substring(request.nik().length() - 4)
                : "");

        return verifyNikUseCase.verifyNik(request)
            .thenApply(ResponseEntity::ok);
    }
}
