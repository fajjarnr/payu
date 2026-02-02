package id.payu.account.adapter.web;

import id.payu.account.domain.port.in.VerifyNikUseCase;
import id.payu.account.dto.VerifyNikRequest;
import id.payu.account.dto.VerifyNikResponse;
import id.payu.security.annotation.Audited;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

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
        operation = Audited.Operation.READ,
        entityType = "NikVerification",
        maskData = true
    )
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
