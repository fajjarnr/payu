package id.payu.account.application.service;

import id.payu.account.domain.model.User;
import id.payu.account.domain.port.in.RegisterUserUseCase;
import id.payu.account.domain.port.out.IdentityProviderPort;
import id.payu.account.domain.port.out.KycVerificationPort;
import id.payu.account.domain.port.out.UserPersistencePort;
import id.payu.account.dto.DukcapilResponse;
import id.payu.account.dto.RegisterUserRequest;
import id.payu.security.annotation.Audited;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import id.payu.account.domain.model.KycStatus;
import id.payu.account.domain.model.UserStatus;
import id.payu.security.annotation.AuditOperation;

@Service
@RequiredArgsConstructor
public class UserApplicationService implements RegisterUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(UserApplicationService.class);

    private final UserPersistencePort userPersistencePort;
    private final KycVerificationPort kycVerificationPort;
    private final IdentityProviderPort identityProviderPort;
    private final id.payu.account.domain.port.out.UserEventPublisherPort userEventPublisherPort;

    @Override
    // @Transactional ensures all JPA calls (existsBy*, save) share the same Hibernate session
    // and JDBC connection with proper transaction management. Without it, the existsBy* calls
    // run in autoCommit mode, polluting the connection state for the subsequent save.
    // KYC call is wrapped in try-catch and fails fast on error, so holding a tx is acceptable.
    // Resilience4j annotations removed — they caused double-fallback (duplicate key errors).
    @Transactional
    @Audited(operation = AuditOperation.CREATE, entityType = "User")
    public CompletableFuture<User> registerUser(RegisterUserRequest command) {
        log.info("Processing registration for user: {}", command.username());

        if (userPersistencePort.existsByEmail(command.email())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userPersistencePort.existsByUsername(command.username())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Step 1: Provision identity in Keycloak BEFORE creating account.
        // If IAM provisioning fails, we don't create an orphaned DB record.
        String iamUserId = identityProviderPort.provisionUser(
            command.username(),
            command.email(),
            command.password(),
            command.fullName()
        );
        // ACCOUNT-005: fail closed — never fall back to the client-supplied
        // externalId (a public caller must not seed identity data). No IAM id,
        // no registration.
        if (iamUserId == null || iamUserId.isBlank()) {
            log.error("IAM provisioning returned no user id for user {} — registration rejected",
                command.username());
            throw new IllegalArgumentException(
                "Identity provider did not return a user id; registration rejected");
        }
        String externalId = iamUserId;

        // Step 2: Attempt KYC verification — if unavailable, register with PENDING status.
        // Simple try-catch replaces resilience4j annotations which caused double-fallback
        // (both @CircuitBreaker and @Bulkhead fired registerFallback, leading to duplicate key errors).
        // Registration is low-throughput and non-idempotent — circuit breaking adds no value here.
        KycStatus kycStatus;
        UserStatus userStatus;
        try {
            DukcapilResponse kycResponse = kycVerificationPort.verifyNik(command.nik(), command.fullName());
            kycStatus = kycResponse.verified() ? KycStatus.APPROVED : KycStatus.REJECTED;
            // BUG-BE-027 Fix: Set user status based on KYC result.
            userStatus = kycResponse.verified()
                    ? UserStatus.ACTIVE
                    : UserStatus.PENDING_VERIFICATION;
        } catch (Exception e) {
            log.warn("KYC service unavailable, registering with PENDING status. Error: {}", e.getMessage());
            kycStatus = KycStatus.PENDING;
            userStatus = UserStatus.PENDING_VERIFICATION;
        }

        User user = User.builder()
            .externalId(externalId)
                .username(command.username())
                .email(command.email())
                .phoneNumber(command.phoneNumber())
                .fullName(command.fullName())
                .nik(command.nik())
                .status(userStatus)
                .kycStatus(kycStatus)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser;
        try {
            savedUser = userPersistencePort.save(user);
            // Publish event (PII-minimized, ACCOUNT-004)
            userEventPublisherPort.publishUserCreated(new id.payu.account.dto.UserCreatedEvent(
                    savedUser.getId(),
                    savedUser.getExternalId(),
                    savedUser.getCreatedAt()));
        } catch (Exception e) {
            // ACCOUNT-005: saga compensation — local persistence failed after IAM
            // provisioning; remove the IAM identity so no orphan Keycloak user
            // lingers. Best-effort: if cleanup fails, the ERROR line is the
            // orphan alert for manual cleanup.
            try {
                identityProviderPort.deleteUser(iamUserId);
            } catch (Exception cleanupFailure) {
                log.error("IAM compensation failed for user {} (iamUserId={}) — manual cleanup required: {}",
                        command.username(), iamUserId, cleanupFailure.getMessage());
            }
            if (e instanceof DataIntegrityViolationException) {
                // BUG-BE-031: Handle race condition where concurrent registrations
                // both pass existsBy checks but one fails at DB unique constraint
                throw new IllegalStateException("Registration conflict: email or username already taken", e);
            }
            throw e;
        }

        log.info("User registered successfully with status={}, kycStatus={}: {}",
                userStatus, kycStatus, savedUser.getId());

        return CompletableFuture.completedFuture(savedUser);
    }
}
