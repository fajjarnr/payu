package id.payu.account.adapter.web;

import id.payu.account.adapter.persistence.repository.UserRepository;
import id.payu.account.adapter.persistence.entity.AccountEntity;
import id.payu.account.adapter.persistence.entity.UserEntity;
import id.payu.account.adapter.persistence.repository.AccountRepository;
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
 *
 * <p>This endpoint is consumed by transaction-service's AccountServiceAdapter
 * to resolve which accounts belong to a given user (by Keycloak externalId/sub).
 * Used for authorization checks in transaction processing.</p>
 */
@RestController
@RequestMapping("/api/v1/accounts/users")
@Tag(name = "UserEntity Accounts", description = "Inter-service account resolution endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserAccountController {

    private static final Logger log = LoggerFactory.getLogger(UserAccountController.class);

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public UserAccountController(AccountRepository accountRepository,
                                 UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns the list of account UUIDs owned by a user.
     *
     * <p>The userId parameter is the Keycloak subject (externalId), NOT the
     * internal database user UUID. This matches how transaction-service resolves
     * account ownership from JWT claims.</p>
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

        // Find internal UserEntity by Keycloak externalId
        Optional<UserEntity> userOpt = userRepository.findByExternalId(userId);
        if (userOpt.isEmpty()) {
            log.debug("No user found for externalId={}, returning empty list", userId);
            return ResponseEntity.ok(Collections.emptyList());
        }

        UserEntity user = userOpt.get();

        // Get all accounts for this user and extract their IDs
        List<UUID> accountIds = accountRepository.findByUserId(user.getId())
                .stream()
                .map(AccountEntity::getId)
                .toList();

        log.debug("Found {} accounts for user externalId={}", accountIds.size(), userId);
        return ResponseEntity.ok(accountIds);
    }
}
