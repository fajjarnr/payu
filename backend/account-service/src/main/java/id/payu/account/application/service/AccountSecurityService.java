package id.payu.account.application.service;

import id.payu.account.domain.port.out.UserPersistencePort;
import id.payu.account.adapter.persistence.repository.UserRepository;
import id.payu.account.domain.model.Account;
import id.payu.account.domain.model.User;
import id.payu.account.domain.port.out.AccountPersistencePort;
import id.payu.account.adapter.persistence.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Security service for account ownership verification.
 * Used in SpEL expressions within @PreAuthorize annotations.
 *
 * <p>Bean name is "accountSecurityService" (Spring default from class name),
 * referenced as {@code @accountSecurityService.isAccountOwner(#accountId, authentication)}
 * in BudgetController.</p>
 */
@Service
public class AccountSecurityService {

    private static final Logger log = LoggerFactory.getLogger(AccountSecurityService.class);

    private final AccountPersistencePort accountPersistencePort;
    private final UserPersistencePort userPersistencePort;

    public AccountSecurityService(AccountPersistencePort accountPersistencePort,
                                  UserPersistencePort userPersistencePort) {
        this.accountPersistencePort = accountPersistencePort;
        this.userPersistencePort = userPersistencePort;
    }

    /**
     * Checks if the authenticated user owns the specified account.
     *
     * @param accountId     the account UUID from the path variable
     * @param authentication the Spring Security Authentication (contains JWT)
     * @return true if the authenticated user owns the account
     */
    public boolean isAccountOwner(UUID accountId, Authentication authentication) {
        if (accountId == null || authentication == null) {
            log.warn("Account ownership check failed: accountId or authentication is null");
            return false;
        }

        try {
            // Extract Keycloak subject (externalId) from JWT
            String externalId;
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                externalId = jwt.getSubject();
            } else {
                externalId = authentication.getName();
            }

            if (externalId == null || externalId.isBlank()) {
                log.warn("Account ownership check failed: no subject in JWT");
                return false;
            }

            // Find the internal User by Keycloak externalId
            Optional<User> userOpt = userPersistencePort.findByExternalId(externalId);
            if (userOpt.isEmpty()) {
                log.warn("Account ownership check failed: no user found for externalId={}", externalId);
                return false;
            }

            User user = userOpt.get();

            // Check if any of the user's accounts match the requested accountId
            List<UUID> accountIds = userPersistencePort.findAccountIdsByUserId(user.getId());
        boolean isOwner = accountIds.contains(accountId);
            

            if (!isOwner) {
                log.warn("Account ownership denied: user {} does not own account {}",
                        externalId, accountId);
            }

            return isOwner;
        } catch (Exception e) {
            log.error("Account ownership check failed with exception: {}", e.getMessage(), e);
            return false;
        }
    }
}
