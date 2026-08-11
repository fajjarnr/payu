package id.payu.wallet.adapter.web;

import id.payu.api.common.response.ApiResponse;
import id.payu.wallet.domain.port.out.JournalPersistencePort;
import id.payu.wallet.dto.LedgerMovementRequest;
import id.payu.wallet.dto.LedgerMovementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PARTNER-PROD-005: trusted-service reconciliation queries against the wallet
 * ledger. Only service clients (Keycloak azp == payu-backend) may read
 * movements by reference; partner-service reconciles its SNAP payment/refund
 * records against these movements.
 */
@RestController
@RequestMapping("/api/v1/reconciliation")
@Tag(name = "Reconciliation", description = "Cross-service ledger reconciliation queries")
@SecurityRequirement(name = "bearerAuth")
public class ReconciliationController {

    private final JournalPersistencePort journalPersistencePort;
    private final String trustedServiceClientId;

    public ReconciliationController(JournalPersistencePort journalPersistencePort,
                                    @Value("${payu.keycloak.client-id:payu-backend}") String trustedServiceClientId) {
        this.journalPersistencePort = journalPersistencePort;
        this.trustedServiceClientId = trustedServiceClientId;
    }

    @PostMapping("/ledger-movements")
    @Operation(summary = "Batch ledger movement lookup by reference IDs (trusted services only)")
    public ResponseEntity<ApiResponse<List<LedgerMovementResponse>>> ledgerMovements(
            @Valid @RequestBody LedgerMovementRequest request) {
        if (!isTrustedServiceRequest()) {
            throw new AccessDeniedException("Only trusted services may query ledger movements");
        }

        List<LedgerMovementResponse> movements = journalPersistencePort
                .findLedgerEntriesByReferenceIds(request.referenceIds())
                .stream()
                .map(LedgerMovementResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(movements));
    }

    private boolean isTrustedServiceRequest() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }
        return trustedServiceClientId.equals(jwt.getClaimAsString("azp"));
    }
}
