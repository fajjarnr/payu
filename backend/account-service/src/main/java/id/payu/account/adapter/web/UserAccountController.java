package id.payu.account.adapter.web;

import id.payu.account.domain.model.User;
import id.payu.account.domain.port.out.UserPersistencePort;
import id.payu.account.interfaces.dto.UserProfileResponse;
import id.payu.api.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for inter-service account queries.
 * SEC-ACCOUNT-001: Ownership or trusted-service check required.
 */
@RestController
@RequestMapping("/api/v1/accounts/users")
@Tag(name = "User Accounts", description = "Inter-service account resolution endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserAccountController {

    private static final Logger log = LoggerFactory.getLogger(UserAccountController.class);

    private final UserPersistencePort userPersistencePort;
    private final String trustedServiceClientId;

    public UserAccountController(UserPersistencePort userPersistencePort,
                                 @Value("${payu.keycloak.client-id:payu-backend}") String trustedServiceClientId) {
        this.userPersistencePort = userPersistencePort;
        this.trustedServiceClientId = trustedServiceClientId;
    }

    private boolean isTrustedServiceRequest() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }
        return trustedServiceClientId.equals(jwt.getClaimAsString("azp"));
    }

    private String extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("No valid JWT authentication found");
        }
        String accountId = jwt.getClaimAsString("account_id");
        return accountId != null ? accountId : jwt.getSubject();
    }

    private void verifyOwnershipOrTrusted(String userId) {
        if (isTrustedServiceRequest()) {
            return;
        }
        String callerId = extractUserId();
        if (!Objects.equals(userId, callerId)) {
            throw new AccessDeniedException("Access denied: you don't own this resource");
        }
    }

    /**
     * Returns the list of account UUIDs owned by a user.
     *
     * @param userId Keycloak subject / externalId
     * @return list of account UUIDs belonging to the user
     */
    @GetMapping("/{userId}/account-ids")
    @Operation(summary = "Get account IDs by user",
               description = "Returns all account UUIDs for a given user (by Keycloak externalId)")
    public ResponseEntity<List<UUID>> getAccountIdsByUserId(
            @Parameter(description = "Keycloak externalId (sub claim)")
            @PathVariable String userId) {

        verifyOwnershipOrTrusted(userId);

        log.debug("Looking up account IDs for user externalId={}", userId);

        Optional<User> userOpt = userPersistencePort.findByExternalId(userId);
        if (userOpt.isEmpty()) {
            log.debug("No user found for externalId={}, returning empty list", userId);
            return ResponseEntity.ok(Collections.emptyList());
        }

        User user = userOpt.get();
        List<UUID> accountIds = userPersistencePort.findAccountIdsByUserId(user.getId());

        log.debug("Found {} accounts for user externalId={}", accountIds.size(), userId);
        return ResponseEntity.ok(accountIds);
    }

    /**
     * Returns the user profile for an externalId (GRPC-008).
     * SEC-ACCOUNT-001: Ownership or trusted-service check. NIK masked in response.
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get user profile by externalId",
               description = "Returns profile data for a user (by Keycloak externalId). NIK is masked.")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @Parameter(description = "Keycloak externalId (sub claim)")
            @PathVariable String userId) {

        verifyOwnershipOrTrusted(userId);

        log.debug("Looking up user profile for externalId={}", userId);

        Optional<User> userOpt = userPersistencePort.findByExternalId(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("ACC_404", "User not found: " + userId));
        }

        return ResponseEntity.ok(ApiResponse.success(UserProfileResponse.fromMasked(userOpt.get())));
    }
}

