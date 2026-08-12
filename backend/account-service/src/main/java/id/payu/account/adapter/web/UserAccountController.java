package id.payu.account.adapter.web;

import id.payu.account.domain.model.User;
import id.payu.account.domain.port.out.UserPersistencePort;
import id.payu.account.dto.UserProfileResponse;
import id.payu.api.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for inter-service account queries.
 * Decoupled from JPA entities and repositories (Hexagonal Architecture).
 */
@RestController
@RequestMapping("/api/v1/accounts/users")
@Tag(name = "User Accounts", description = "Inter-service account resolution endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserAccountController {

    private static final Logger log = LoggerFactory.getLogger(UserAccountController.class);

    private final UserPersistencePort userPersistencePort;

    public UserAccountController(UserPersistencePort userPersistencePort) {
        this.userPersistencePort = userPersistencePort;
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
     * Used by lending-service enhanced credit scoring (kycStatus, account tenure).
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get user profile by externalId",
               description = "Returns profile data for a user (by Keycloak externalId)")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @Parameter(description = "Keycloak externalId (sub claim)")
            @PathVariable String userId) {

        log.debug("Looking up user profile for externalId={}", userId);

        Optional<User> userOpt = userPersistencePort.findByExternalId(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("ACC_404", "User not found: " + userId));
        }

        return ResponseEntity.ok(ApiResponse.success(UserProfileResponse.from(userOpt.get())));
    }
}
